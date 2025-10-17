package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.*;
import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import com.cartagenacorp.lm_issues.exceptions.BaseException;
import com.cartagenacorp.lm_issues.repository.DescriptionRepository;
import com.cartagenacorp.lm_issues.repository.specifications.IssueSpecifications;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import com.cartagenacorp.lm_issues.util.JwtContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueService {

    private static final Logger logger = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final DescriptionRepository descriptionRepository;
    private final IssueMapper issueMapper;
    private final UserExternalService userExternalService;
    private final ProjectExternalService projectExternalService;
    private final AuditExternalService auditExternalService;
    private final NotificationExternalService notificationExternalService;
    private final FileStorageService fileStorageService;

    public IssueService(IssueRepository issueRepository, DescriptionRepository descriptionRepository, IssueMapper issueMapper, UserExternalService userExternalService,
                        ProjectExternalService projectExternalService, AuditExternalService auditExternalService, NotificationExternalService notificationExternalService, FileStorageService fileStorageService) {
        this.issueRepository = issueRepository;
        this.descriptionRepository = descriptionRepository;
        this.issueMapper = issueMapper;
        this.userExternalService = userExternalService;
        this.projectExternalService = projectExternalService;
        this.auditExternalService = auditExternalService;
        this.notificationExternalService = notificationExternalService;
        this.fileStorageService = fileStorageService;
    }

    public void addFilesToDescription(UUID issueId, UUID descriptionId, MultipartFile[] files) {
        logger.info("[IssueService] [addFilesToDescription] Iniciando proceso para adjuntar archivos a la descripción ID={} del issue ID={}", descriptionId, issueId);

        if (!issueRepository.existsById(issueId)) {
            logger.warn("[IssueService] [addFilesToDescription] No se encontró la issue con ID={}", issueId);
            throw new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
        }

        Description description = descriptionRepository.findById(descriptionId)
                .orElseThrow(() -> {
                    logger.warn("[IssueService] [addFilesToDescription] No se encontró la descripción con ID={}", descriptionId);
                    return new BaseException("Descripción no encontrada", HttpStatus.NOT_FOUND.value());
                });

        if (!description.getIssue().getId().equals(issueId)) {
            logger.warn("[IssueService] [addFilesToDescription] La descripción ID={} no pertenece a la issue ID={}", descriptionId, issueId);
            throw new BaseException("La descripción no pertenece a esta Issue", HttpStatus.BAD_REQUEST.value());
        }

        if (files == null || files.length == 0) {
            logger.info("[IssueService] [addFilesToDescription] No se recibieron archivos para adjuntar a la descripción con ID={}", descriptionId);
            return;
        }

        logger.info("[IssueService] [addFilesToDescription] Guardando {} archivo(s) en la descripción con ID={}", files.length, descriptionId);
        fileStorageService.saveFiles(description, files);
        logger.info("[IssueService] [addFilesToDescription] Archivos adjuntados exitosamente a la descripción ID={} del issue ID={}", descriptionId, issueId);
    }

    @Transactional
    public IssueDtoResponse createIssue(IssueDtoRequest issueDtoRequest) {
        logger.info("[IssueService] [createIssue] Iniciando creación de una nueva Issue");

        if (issueDtoRequest == null) {
            logger.warn("[IssueService] [createIssue] La solicitud de creación de Issue es nula");
            throw new BaseException("La Issue no puede ser nula", HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueService] [createIssue] Usuario solicitante ID={}, Organización ID={}", userId, organizationId);

        if (!projectExternalService.validateProjectExists(issueDtoRequest.getProjectId(), token)) {
            logger.warn("[IssueService] [createIssue] El proyecto con ID={} no existe", issueDtoRequest.getProjectId());
            throw new BaseException("El ID del proyecto proporcionado no es válido", HttpStatus.NOT_FOUND.value());
        }

        if (!projectExternalService.validateProjectParticipant(issueDtoRequest.getProjectId(), token)) {
            logger.warn("[IssueService] [createIssue] El usuario ID={} no es participante del proyecto ID={}", userId, issueDtoRequest.getProjectId());
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        if (issueDtoRequest.getAssignedId() != null &&
                !userExternalService.userExists(issueDtoRequest.getAssignedId(), token)) {
            logger.warn("[IssueService] [createIssue] El usuario asignado con ID={} no existe", issueDtoRequest.getAssignedId());
            throw new BaseException("Usuario asignado no encontrado", HttpStatus.NOT_FOUND.value());
        }

        logger.info("[IssueService] [createIssue] Creando entidad Issue a partir del DTO");
        Issue issue = issueMapper.toEntity(issueDtoRequest);
        issue.setReporterId(userId);
        issue.setOrganizationId(organizationId);
        issueRepository.save(issue);

        logger.info("[IssueService] [createIssue] Enlazando descripciones y guardando Issue en base de datos");
        issueMapper.linkDescriptions(issue);
        Issue savedIssue = issueRepository.save(issue);

        try {
            auditExternalService.logChange(savedIssue.getId(), savedIssue.getTitle(), userId, "CREATE", "Nueva Issue", savedIssue.getProjectId(), savedIssue, null, token);
            logger.info("[IssueService] [createIssue] Registro de auditoría enviado correctamente para la Issue con ID={}", savedIssue.getId());
        } catch (Exception ex){
            logger.error("[IssueService] [createIssue] Error al registrar auditoría para la Issue con ID={}: {}", savedIssue.getId(), ex.getMessage());
        }

        if (savedIssue.getAssignedId() != null) {
            logger.info("[IssueService] [createIssue] Registrando notificación para el usuario asignado ID={}", savedIssue.getAssignedId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationExternalService.sendNotification(
                                savedIssue.getAssignedId(),
                                "Se ha creado una nueva Issue a la que estás asignado: " + savedIssue.getTitle(),
                                "ISSUE_ASSIGNED",
                                Map.of(
                                        "issueId", savedIssue.getId().toString(),
                                        "projectId", savedIssue.getProjectId().toString()
                                ),
                                savedIssue.getProjectId(),
                                savedIssue.getId()
                        );
                    } catch (Exception ex) {
                        logger.error("[IssueService] [createIssue] Error al enviar notificación al usuario asignado: {}", ex.getMessage());
                    }
                }
            });
        } else {
            logger.info("[IssueService] [createIssue] La Issue no tiene usuario asignado, no se enviará notificación");
        }
        logger.info("[IssueService] [createIssue] Issue creada exitosamente con ID={}", savedIssue.getId());
        return getIssueDtoResponse(issue);
    }

    @Transactional
    public List<IssueDTO> createIssuesBatch(List<IssueDTO> issues) {
        logger.info("[IssueService] [createIssuesBatch] Iniciando creación en lote de Issues");

        if (issues == null || issues.isEmpty()) {
            logger.warn("[IssueService] [createIssuesBatch] La lista de Issues está vacía o es nula");
            throw new BaseException("La lista de Issues no puede estar vacía", HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = JwtContextHolder.getUserId();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueService] [createIssuesBatch] Usuario solicitante ID={}, Organización ID={}, Cantidad de Issues={}", userId, organizationId, issues.size());

        List<Issue> entities = new ArrayList<>();


        for (IssueDTO issueDTO : issues) {
            issueDTO.setReporterId(userId);
            issueDTO.setOrganizationId(organizationId);
           // if (issueDTO.getDescriptionsDTO() == null) { issueDTO.setDescriptionsDTO(new ArrayList<>()); }
            Issue issue = issueMapper.toEntityImport(issueDTO);
            issue.getDescriptions().forEach(description -> description.setIssue(issue));
            entities.add(issue);

            logger.debug("[IssueService] [createIssuesBatch] Issue preparada para guardar: título='{}', proyecto ID={}", issue.getTitle(), issue.getProjectId());
        }

        logger.info("[IssueService] [createIssuesBatch] Guardando {} Issues en base de datos...", entities.size());
        List<Issue> saved;
        try {
            saved = issueRepository.saveAll(entities);
            logger.info("[IssueService] [createIssuesBatch] Se guardaron correctamente {} Issues", saved.size());
        } catch (Exception e) {
            logger.error("[IssueService] [createIssuesBatch] Error al guardar Issues en base de datos: {}", e.getMessage(), e);
            throw new BaseException("Error al guardar las Issues en la base de datos", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        List<IssueDTO> result = saved.stream().map(issueMapper::toDtoImport).toList();
        logger.info("[IssueService] [createIssuesBatch] Creación en lote de Issues completada exitosamente, Issues creadas={}", result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> findIssues(String keyword, UUID projectId, UUID sprintId, Long status,
                                                        Long priority, Long type, List<UUID> assignedIds,
                                                        Boolean isParent, Pageable pageable) {

        logger.info("[IssueService] [findIssues] Iniciando búsqueda de Issues con filtros. Proyecto ID={}, Sprint ID={}, Estado ID={}, Prioridad ID={}, Tipo ID={}",
                projectId, sprintId, status, priority, type);

        if (!projectExternalService.validateProjectParticipant(projectId, JwtContextHolder.getToken())) {
            logger.warn("[IssueService] [findIssues] El usuario no es participante del proyecto con ID={}", projectId);
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        logger.debug("[IssueService] [findIssues] Construyendo especificación de búsqueda...");
        Specification<Issue> spec = Specification
                .where(IssueSpecifications.searchByKeyword(keyword))
                .and(IssueSpecifications.hasProject(projectId))
                .and(IssueSpecifications.hasSprint(sprintId))
                .and(IssueSpecifications.hasStatus(status))
                .and(IssueSpecifications.hasPriority(priority))
                .and(IssueSpecifications.hasType(type))
                .and(IssueSpecifications.hasAssignedIn(assignedIds))
                .and(IssueSpecifications.hasParentCondition(isParent));

        Page<Issue> issues;
        try {
            issues = issueRepository.findAll(spec, pageable);
            logger.info("[IssueService] [findIssues] Se encontraron {} Issues en la búsqueda", issues.getTotalElements());
        } catch (Exception e) {
            logger.error("[IssueService] [findIssues] Error al ejecutar la consulta de Issues: {}", e.getMessage(), e);
            throw new BaseException("Error al obtener las Issues", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        Set<UUID> userIds = new HashSet<>();
        issues.getContent().forEach(issue -> {
            if (issue.getAssignedId() != null) userIds.add(issue.getAssignedId());
            if (issue.getReporterId() != null) userIds.add(issue.getReporterId());
        });
        logger.debug("[IssueService] [findIssues] Se recolectaron {} IDs de usuarios relacionados con las Issues", userIds.size());

        List<UserBasicDataDto> usersOpt;
        try {
            usersOpt = userExternalService.getUsersData(
                    JwtContextHolder.getToken(),
                    userIds.stream().map(UUID::toString).collect(Collectors.toList())
            );
            logger.info("[IssueService] [findIssues] Datos de usuarios obtenidos exitosamente ({} usuarios)", usersOpt.size());
        } catch (Exception e) {
            logger.warn("[IssueService] [findIssues] No se pudieron obtener datos de usuarios: {}", e.getMessage());
            usersOpt = Collections.emptyList();
        }

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        Page<IssueDtoResponse> mappedPage = issues.map(issue -> getIssueDtoResponse(userMap, issue));
        logger.info("[IssueService] [findIssues] Mapeo de Issues completado. Total de páginas devueltas: {}", mappedPage.getTotalPages());

        logger.info("[IssueService] [findIssues] Finalizando búsqueda de Issues correctamente");
        return new PageResponseDTO<>(mappedPage);
    }

    @Transactional(readOnly = true)
    public IssueDtoResponse getIssueById(UUID id) {
        logger.info("[IssueService] [getIssueById] Consultando issue con ID={}", id);

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[IssueService] [getIssueById] No se encontró la issue con ID={}", id);
                    return new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
                });

        logger.info("[IssueService] [getIssueById] Issue encontrada con ID={}, validando participación en el proyecto ID={}", issue.getId(), issue.getProjectId());

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            logger.warn("[IssueService] [getIssueById] El usuario no es participante del proyecto con ID={}", issue.getProjectId());
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        logger.info("[IssueService] [getIssueById] Retornando información de la issue con ID={}", issue.getId());
        return getIssueDtoResponse(issue);
    }

    @Transactional
    public IssueDtoResponse updateIssue(UUID id, IssueDtoRequest updatedIssueDTO) {
        logger.info("[IssueService] [updateIssue] Iniciando actualización de issue con ID={}", id);

        if (updatedIssueDTO == null) {
            logger.warn("[IssueService] [updateIssue] La Issue proporcionada es nula");
            throw new BaseException("La Issue no puede ser nula", HttpStatus.BAD_REQUEST.value());
        }

        String token = JwtContextHolder.getToken();
        UUID userId = JwtContextHolder.getUserId();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueService] [createIssue] Usuario solicitante ID={}, Organización ID={}", userId, organizationId);

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[IssueService] [updateIssue] Issue no encontrada con ID={}", id);
                    return new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
                });
        Issue originalIssue = new Issue(issue);

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            logger.warn("[IssueService] [updateIssue] El usuario no es participante del proyecto con ID={}", issue.getProjectId());
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        if (updatedIssueDTO.getProjectId() != null && !updatedIssueDTO.getProjectId().equals(issue.getProjectId())) {
            logger.warn("[IssueService] [updateIssue] Se intentó modificar el ID del proyecto, operación no permitida");
            throw new BaseException("El ID del proyecto no puede cambiar", HttpStatus.BAD_REQUEST.value());
        }

        if (updatedIssueDTO.getAssignedId() != null) {
            logger.warn("[IssueService] [updateIssue] Se intentó modificar el usuario asignado, operación no permitida");
            throw new BaseException("El campo de usuario asignado no es aceptado", HttpStatus.BAD_REQUEST.value());
        }

        List<String> changedFields = new ArrayList<>();
        AtomicBoolean descriptionsChanged = new AtomicBoolean(false);

        logger.debug("[IssueService] [updateIssue] Iniciando comparación de campos para detectar cambios");

        if (!Objects.equals(issue.getTitle(), updatedIssueDTO.getTitle())) {
            changedFields.add("title");
            issue.setTitle(updatedIssueDTO.getTitle());
        }
        if (!Objects.equals(issue.getEstimatedTime(), updatedIssueDTO.getEstimatedTime())) {
            changedFields.add("estimatedTime");
            issue.setEstimatedTime(updatedIssueDTO.getEstimatedTime());
        }
        if (!Objects.equals(issue.getPriority(), updatedIssueDTO.getPriority())) {
            changedFields.add("priority");
            issue.setPriority(updatedIssueDTO.getPriority());
        }
        if (!Objects.equals(issue.getStatus(), updatedIssueDTO.getStatus())) {
            changedFields.add("status");
            issue.setStatus(updatedIssueDTO.getStatus());
        }
        if (!Objects.equals(issue.getType(), updatedIssueDTO.getType())) {
            changedFields.add("type");
            issue.setType(updatedIssueDTO.getType());
        }
        if (!Objects.equals(issue.getStartDate(), updatedIssueDTO.getStartDate())) {
            changedFields.add("startDate");
            issue.setStartDate(updatedIssueDTO.getStartDate());
        }
        if (!Objects.equals(issue.getEndDate(), updatedIssueDTO.getEndDate())) {
            changedFields.add("endDate");
            issue.setEndDate(updatedIssueDTO.getEndDate());
        }
        if (!Objects.equals(issue.getRealDate(), updatedIssueDTO.getRealDate())) {
            changedFields.add("realDate");
            issue.setRealDate(updatedIssueDTO.getRealDate());
        }

        if (updatedIssueDTO.getDescriptions() != null) {
            logger.debug("[IssueService] [updateIssue] Procesando descripciones y archivos adjuntos");

            for (DescriptionDtoRequest descriptionDtoRequest : updatedIssueDTO.getDescriptions()) {
                Description description;

                if (descriptionDtoRequest.getId() != null) {
                    description = issue.getDescriptions().stream()
                            .filter(d -> d.getId().equals(descriptionDtoRequest.getId()))
                            .findFirst()
                            .orElseThrow(() -> {
                                logger.warn("[IssueService] [updateIssue] Descripción con ID={} no encontrada", descriptionDtoRequest.getId());
                                return new BaseException("Descripcion no encontrada", HttpStatus.NOT_FOUND.value());
                            });

                    if (!Objects.equals(description.getText(), descriptionDtoRequest.getText()) ||
                            !Objects.equals(description.getTitle(), descriptionDtoRequest.getTitle())) {
                        description.setTitle(descriptionDtoRequest.getTitle());
                        description.setText(descriptionDtoRequest.getText());
                        descriptionsChanged.set(true);
                    }

                } else {
                    logger.debug("[IssueService] [updateIssue] Agregando nueva descripción");
                    description = new Description();
                    description.setTitle(descriptionDtoRequest.getTitle());
                    description.setText(descriptionDtoRequest.getText());
                    description.setIssue(issue);
                    issue.getDescriptions().add(description);
                    descriptionsChanged.set(true);
                }

                if (descriptionDtoRequest.getAttachments() != null) {
                    Set<UUID> incomingFileIds = descriptionDtoRequest.getAttachments().stream()
                            .filter(f -> f.getId() != null)
                            .map(DescriptionFileDtoRequest::getId)
                            .collect(Collectors.toSet());

                    Iterator<DescriptionFile> iterator = description.getAttachments().iterator();
                    while (iterator.hasNext()) {
                        DescriptionFile existingFile = iterator.next();
                        if (!incomingFileIds.contains(existingFile.getId())) {
                            logger.info("[IssueService] [updateIssue] Eliminando archivo con ID={} y URL={}", existingFile.getId(), existingFile.getFileUrl());
                            fileStorageService.deleteFile(existingFile.getFileUrl());
                            iterator.remove();
                            descriptionsChanged.set(true);
                        }
                    }

                    for (DescriptionFileDtoRequest fileDTO : descriptionDtoRequest.getAttachments()) {
                        if (fileDTO.getId() == null) {
                            logger.info("[IssueService] [updateIssue] Agregando nuevo archivo adjunto a la descripción '{}'", description.getTitle());
                            DescriptionFile newFile = new DescriptionFile();
                            newFile.setFileName(fileDTO.getFileName());
                            newFile.setFileUrl(fileDTO.getFileUrl());
                            newFile.setDescription(description);
                            description.getAttachments().add(newFile);
                            descriptionsChanged.set(true);
                        }
                    }
                }
            }

            boolean removed = issue.getDescriptions().removeIf(
                    description -> description.getId() != null && updatedIssueDTO.getDescriptions().stream()
                            .noneMatch(descriptionDtoRequest ->
                                    descriptionDtoRequest.getId() != null && descriptionDtoRequest.getId().equals(description.getId())
                            )
            );
            if (removed) {
                logger.info("[IssueService] [updateIssue] Se eliminaron descripciones no presentes en la solicitud");
                descriptionsChanged.set(true);
            }

            if (descriptionsChanged.get()) {
                changedFields.add("descriptions");
                issue.setUpdatedAt(LocalDateTime.now());
            }
        }

        Issue savedIssue = issueRepository.save(issue);
        logger.info("[IssueService] [updateIssue] Issue con ID={} actualizada correctamente", savedIssue.getId());

        if (!changedFields.isEmpty()) {
            String auditDesc = "Updated fields: " + String.join(", ", changedFields);
            logger.info("[IssueService] [updateIssue] Campos modificados: {}", auditDesc);

            try {
                auditExternalService.logChange(savedIssue.getId(), savedIssue.getTitle(), userId, "UPDATE", "Issue editada -> " + auditDesc, savedIssue.getProjectId(), originalIssue, savedIssue, token);
                logger.info("[IssueService] [updateIssue] Registro de auditoría enviado correctamente para la Issue con ID={}", savedIssue.getId());
            } catch (Exception e) {
                logger.error("[IssueService] [updateIssue] Error al registrar auditoría: {}", e.getMessage());
            }

            if (savedIssue.getAssignedId() != null) {
                String message;
                if (savedIssue.getParent() != null) {
                    message = "Se ha actualizado una Subtarea a la que estás asignado: " + savedIssue.getTitle();
                } else {
                    message = "Se ha actualizado una Issue a la que estás asignado: " + savedIssue.getTitle();
                }
                try {
                    notificationExternalService.sendNotification(
                            savedIssue.getAssignedId(),
                            message,
                            "ISSUE_UPDATED",
                            Map.of(
                                    "issueId", savedIssue.getId().toString(),
                                    "projectId", savedIssue.getProjectId().toString()
                            ),
                            savedIssue.getProjectId(),
                            savedIssue.getId()
                    );
                    logger.info("[IssueService] [updateIssue] Notificación enviada al usuario asignado con ID={}", savedIssue.getAssignedId());
                } catch (Exception e) {
                    logger.error("[IssueService] [updateIssue] Error al enviar notificación al usuario asignado: {}", e.getMessage());
                }
            }
        }

        logger.info("[IssueService] [updateIssue] Finalizando actualización de issue con ID={}", id);
        return getIssueDtoResponse(savedIssue);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        logger.info("[IssueService] [deleteIssue] Iniciando eliminación de issue con ID={}", id);

        String token = JwtContextHolder.getToken();
        UUID userId = JwtContextHolder.getUserId();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueService] [deleteIssue] Usuario solicitante ID={}, Organización ID={}", userId, organizationId);

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("[IssueService] [deleteIssue] Issue con ID={} no encontrada", id);
                    return new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
                });

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            logger.warn("[IssueService] [deleteIssue] El usuario no es participante del proyecto con ID={}", issue.getProjectId());
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        issueRepository.delete(issue);
        logger.info("[IssueService] [deleteIssue] Issue con ID={} eliminada correctamente", id);

        try {
            auditExternalService.logChange(issue.getId(), issue.getTitle(), userId, "DELETE", "Issue eliminada", issue.getProjectId(), issue, null, token);
            logger.info("[IssueService] [deleteIssue] Registro de auditoría enviado correctamente para la Issue con ID={}", id);
        } catch (Exception e) {
            logger.error("[IssueService] [deleteIssue] Error al registrar auditoría: {}", e.getMessage());
        }

        logger.info("[IssueService] [deleteIssue] Finalizando proceso de eliminación de issue con ID={}", id);
    }

    @Transactional
    public void deleteIssues(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            logger.warn("[IssueService] [deleteIssues] La lista de IDs está vacía o es nula.");
            throw new BaseException("La lista de IDs no puede estar vacía", HttpStatus.BAD_REQUEST.value());
        }
        logger.info("[IssueService] [deleteIssues] Iniciando eliminación masiva de issues. Cantidad de IDs recibidos={}", (ids != null ? ids.size() : 0));

        String token = JwtContextHolder.getToken();
        UUID userId = JwtContextHolder.getUserId();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueService] [deleteIssues] Usuario solicitante ID={}, Organización ID={}", userId, organizationId);

        List<Issue> issues = issueRepository.findAllById(ids);

        if (issues.size() != ids.size()) {
            logger.warn("[IssueService] [deleteIssues] Algunas issues no fueron encontradas. Esperadas: {}, Encontradas: {}", ids.size(), issues.size());
            throw new BaseException("Algunas de las Issues que se intentan eliminar no existen", HttpStatus.NOT_FOUND.value());
        }

        Set<UUID> projectIds = issues.stream()
                .map(Issue::getProjectId)
                .collect(Collectors.toSet());

        for (UUID projectId : projectIds) {
            if (!projectExternalService.validateProjectParticipant(projectId, token)) {
                logger.warn("[IssueService] [deleteIssues] El usuario no es participante del proyecto con ID={}", projectId);
                throw new BaseException("No eres participante en uno o más proyectos de las issues seleccionadas", HttpStatus.FORBIDDEN.value());
            }
        }

        logger.info("[IssueService] [deleteIssues] Eliminando {} issues de la base de datos...", issues.size());
        issueRepository.deleteAll(issues);
        logger.info("[IssueService] [deleteIssues] Issues eliminadas correctamente.");

        try {
            for (Issue issue : issues) {
                auditExternalService.logChange(issue.getId(), issue.getTitle(), userId, "DELETE", "Issue eliminada en eliminación masiva", issue.getProjectId(), issue, null, token);
                logger.debug("[IssueService] [deleteIssues] Registro de auditoría enviado correctamente para la Issue con ID={}", issue.getId());
            }
        } catch (Exception e) {
            logger.error("[IssueService] [deleteIssues] Error al registrar auditoría: {}", e.getMessage());
        }

        logger.info("[IssueService] [deleteIssues] Finalizando proceso de eliminación masiva de issues.");
    }

    @Transactional
    public IssueDtoResponse assignUserToIssue(UUID issueId, UUID assignedId) {
        logger.info("[IssueService] [assignUserToIssue] Iniciando asignación de usuario ID={} al Issue ID={}", assignedId, issueId);

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> {
                    logger.warn("[IssueService] [assignUserToIssue] Issue con ID={} no encontrada", issueId);
                    return new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
                });
        Issue originalIssue = new Issue(issue);

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), token)) {
            logger.warn("[IssueService] [deleteIssue] El usuario no es participante del proyecto con ID={}", issue.getProjectId());
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        String auditDescription;
        if (assignedId == null) {
            issue.setAssignedId(null);
            auditDescription = "Usuario desasignado de la issue";
            logger.info("[IssueService] [assignUserToIssue] Usuario desasignado de la issue con ID={}", issueId);
        } else {
            if (!userExternalService.userExists(assignedId, token)) {
                logger.warn("[IssueService] [assignUserToIssue] Usuario asignado no encontrado. assignedId={}", assignedId);
                throw new BaseException("Usuario asignado no encontrado", HttpStatus.NOT_FOUND.value());
            }
            issue.setAssignedId(assignedId);
            auditDescription = "Usuario asignado a la issue";
            logger.info("[IssueService] [assignUserToIssue] Usuario ID={} asignado a la issue ID={}", assignedId, issueId);

        }

        Issue savedIssue = issueRepository.save(issue);
        logger.debug("[IssueService] [assignUserToIssue] Issue con ID={} actualizada y guardada correctamente en base de datos", savedIssue.getId());

        try {
            auditExternalService.logChange(savedIssue.getId(), savedIssue.getTitle(), userId, "ASSIGN", auditDescription, savedIssue.getProjectId(), originalIssue, savedIssue, token);
            logger.info("[IssueService] [assignUserToIssue] Registro de auditoría enviado correctamente para la Issue con ID={}", savedIssue.getId());
        } catch (Exception e) {
            logger.error("[IssueService] [assignUserToIssue] Error al registrar auditoría: {}", e.getMessage());
        }

        if (savedIssue.getAssignedId() != null) {
            String message;
            if (savedIssue.getParent() != null) {
                message = "Se le ha asignado una Subtarea: " + savedIssue.getTitle();
            } else {
                message = "Se le ha asignado una Issue: " + savedIssue.getTitle();
            }
            try {
                notificationExternalService.sendNotification(
                        savedIssue.getAssignedId(),
                        message,
                        "ISSUE_ASSIGNED",
                        Map.of(
                                "issueId", savedIssue.getId().toString(),
                                "projectId", savedIssue.getProjectId().toString()
                        ),
                        savedIssue.getProjectId(),
                        savedIssue.getId()
                );
                logger.info("[IssueService] [assignUserToIssue] Notificación enviada al usuario asignado con ID={}", savedIssue.getAssignedId());
            } catch (Exception e) {
                logger.error("[IssueService] [assignUserToIssue] Error al enviar notificación al usuario asignado: {}", e.getMessage());
            }
        }

        logger.info("[IssueService] [assignUserToIssue] Proceso de asignación de usuario finalizado correctamente al Issue con ID={}", issueId);
        return issueMapper.toDto(savedIssue);
    }

    private IssueDtoResponse getIssueDtoResponse(Map<UUID, UserBasicDataDto> userMap, Issue issue) {
        IssueDtoResponse issueDtoResponse = issueMapper.toDto(issue);

        issueDtoResponse.setReporterId(userMap.getOrDefault(issue.getReporterId(),
                new UserBasicDataDto(issue.getReporterId(), null, null, null, null, null)));


        issueDtoResponse.setAssignedId(userMap.getOrDefault(issue.getAssignedId(),
                new UserBasicDataDto(issue.getAssignedId(), null, null, null, null, null)));

        if (issue.getParent() != null) {
            Issue parent = issue.getParent();
            issueDtoResponse.setParent(new ParentInfoDto(parent.getId(), parent.getTitle()));
        }

        return issueDtoResponse;
    }

    private IssueDtoResponse getIssueDtoResponse(Issue issue) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(issue.getReporterId());
        if (issue.getAssignedId() != null) { userIds.add(issue.getAssignedId());}

        List<UserBasicDataDto> usersOpt = userExternalService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream().map(UUID::toString).toList()
        );

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        return getIssueDtoResponse(userMap, issue);
    }

    @Transactional(readOnly = true)
    public boolean issueExists(UUID id){
        logger.info("[IssueService] [issueExists] Verificando si existe el Issue con ID={}", id);
        return issueRepository.existsById(id);
    }

    @Transactional
    public void assignIssuesToSprint(List<UUID> issueIds, UUID sprintId) {
        logger.info("[IssueService] [assignIssuesToSprint] Iniciando asignación de {} issues al sprint con ID: {}", issueIds.size(), sprintId);

        String token = JwtContextHolder.getToken();
        UUID userId = JwtContextHolder.getUserId();

        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            logger.warn("[IssueService] [assignIssuesToSprint] Algunas issues no fueron encontradas. Esperadas: {}, Encontradas: {}", issueIds.size(), issues.size());
            throw new BaseException("Algunas Issues no fueron encontradas", HttpStatus.NOT_FOUND.value());
        }

        for (Issue issue : issues) {
            issue.setSprintId(sprintId);
        }

        issueRepository.saveAll(issues);
        logger.info("[IssueService] [assignIssuesToSprint] Issues guardadas exitosamente.");

        for (Issue issue : issues) {
            try {
                auditExternalService.logChange(issue.getId(), issue.getTitle(), userId, "SPRINT_ASSIGN", "Sprint asignado: " + sprintId, issue.getProjectId(), null, issue, token);
                logger.debug("[IssueService] [assignIssuesToSprint] Registro de auditoría enviado correctamente para la Issue con ID={}", issue.getId());
            } catch (Exception ex) {
                logger.error("[IssueService] [assignIssuesToSprint] Error al registrar auditoría: {}", ex.getMessage());
            }
        }
        logger.info("[IssueService] [assignIssuesToSprint] Asignación de issues al Sprint con ID={} completada exitosamente", sprintId);
    }

    @Transactional
    public void removeIssuesFromSprint(List<UUID> issueIds) {
        logger.info("[IssueService] [removeIssuesFromSprint] Iniciando eliminación de {} issues del sprint", issueIds.size());

        String token = JwtContextHolder.getToken();
        UUID userId = JwtContextHolder.getUserId();

        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            logger.warn("[IssueService] [removeIssuesFromSprint] Algunas issues no fueron encontradas. Esperadas: {}, Encontradas: {}", issueIds.size(), issues.size());
            throw new BaseException("Algunas Issues no fueron encontradas", HttpStatus.NOT_FOUND.value());
        }

        for (Issue issue : issues) {
            issue.setSprintId(null);
        }

        issueRepository.saveAll(issues);
        logger.info("[IssueService] [removeIssuesFromSprint] Issues actualizadas correctamente.");

        for (Issue issue : issues) {
            logger.debug("[IssueService] [removeIssuesFromSprint] Eliminando el sprint de la Issue con ID={}", issue.getId());
            try {
                auditExternalService.logChange(issue.getId(), issue.getTitle(), userId, "SPRINT_REMOVE", "Sprint removido de la issue", issue.getProjectId(), null, issue, token);
                logger.debug("[IssueService] [removeIssuesFromSprint] Registro de auditoría enviado correctamente para la Issue con ID={}", issue.getId());
            } catch (Exception ex) {
                logger.error("[IssueService] [removeIssuesFromSprint] Error al registrar auditoría: {}", ex.getMessage());
            }
        }
        logger.info("[IssueService] [removeIssuesFromSprint] Eliminación de issues del sprint completada exitosamente");
    }
}