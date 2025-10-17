package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.IssueDtoRequest;
import com.cartagenacorp.lm_issues.dto.IssueDtoResponse;
import com.cartagenacorp.lm_issues.dto.IssueRelationDto;
import com.cartagenacorp.lm_issues.dto.UserBasicDataDto;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.entity.IssueRelation;
import com.cartagenacorp.lm_issues.exceptions.BaseException;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.mapper.IssueRelationMapper;
import com.cartagenacorp.lm_issues.repository.IssueRelationRepository;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import com.cartagenacorp.lm_issues.util.JwtContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueRelationService {

    private static final Logger logger = LoggerFactory.getLogger(IssueRelationService.class);

    private final IssueRepository issueRepository;
    private final IssueRelationRepository issueRelationRepository;
    private final IssueMapper issueMapper;
    private final IssueRelationMapper issueRelationMapper;
    private final UserExternalService userExternalService;
    private final AuditExternalService auditExternalService;
    private final NotificationExternalService notificationExternalService;

    public IssueRelationService(IssueRepository issueRepository, IssueRelationRepository issueRelationRepository, IssueMapper issueMapper, IssueRelationMapper issueRelationMapper,
                                UserExternalService userExternalService, AuditExternalService auditExternalService, NotificationExternalService notificationExternalService) {
        this.issueRepository = issueRepository;
        this.issueRelationRepository = issueRelationRepository;
        this.issueMapper = issueMapper;
        this.issueRelationMapper = issueRelationMapper;
        this.userExternalService = userExternalService;
        this.auditExternalService = auditExternalService;
        this.notificationExternalService = notificationExternalService;
    }

    @Transactional
    public IssueDtoResponse createSubtask(UUID parentId, IssueDtoRequest subtask) {
        logger.info("[IssueRelationService] [createSubtask] Iniciando creación de una nueva Subtask para el Issue padre con ID={}", parentId);

        Issue parent = issueRepository.findById(parentId)
                .orElseThrow(() -> {
                    logger.warn("[IssueRelationService] [createSubtask] No se encontró el issue padre con ID={}", parentId);
                    return new BaseException("No se encontró el Issue principal", HttpStatus.NOT_FOUND.value());
                });

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        logger.info("[IssueRelationService] [createSubtask] Usuario solicitante ID={}, Organización ID={}", userId, organizationId);

        logger.info("[IssueRelationService] [createSubtask] Creando entidad Issue(Subtask) a partir del DTO");
        Issue subtaskEntity = issueMapper.toEntity(subtask);
        subtaskEntity.setParent(parent);
        subtaskEntity.setProjectId(parent.getProjectId());
        subtaskEntity.setReporterId(JwtContextHolder.getUserId());
        subtaskEntity.setOrganizationId(parent.getOrganizationId());
        subtaskEntity.setSprintId(null); // Las subtasks no pueden estar en un sprint
        issueRepository.save(subtaskEntity);
        logger.info("[IssueRelationService] [createSubtask] Issue(Subtask) guardada ID={} para el proyecto ID={}", subtaskEntity.getId(), subtaskEntity.getProjectId());

        try {
            auditExternalService.logChange(subtaskEntity.getId(), subtaskEntity.getTitle(), userId, "CREATE", "Nueva Subtask", subtaskEntity.getProjectId(), subtaskEntity, null, token);
            logger.info("[IssueRelationService] [createSubtask] Registro de auditoría enviado correctamente para la Subtask con ID={}", subtaskEntity.getId());
        } catch (Exception ex){
            logger.error("[IssueRelationService] [createSubtask] Error al registrar auditoría para la Subtask con ID={}: {}", subtaskEntity.getId(), ex.getMessage());
        }

        if (subtaskEntity.getAssignedId() != null) {
            logger.info("[IssueRelationService] [createSubtask] Registrando notificación para el usuario asignado ID={}", subtaskEntity.getAssignedId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationExternalService.sendNotification(
                                subtaskEntity.getAssignedId(),
                                "Se ha creado una nueva Subtarea a la que estás asignado: " + subtaskEntity.getTitle(),
                                "ISSUE_ASSIGNED",
                                Map.of(
                                        "issueId", subtaskEntity.getId().toString(),
                                        "projectId", subtaskEntity.getProjectId().toString()
                                ),
                                subtaskEntity.getProjectId(),
                                subtaskEntity.getId()
                        );
                    } catch (Exception ex) {
                        logger.error("[IssueRelationService] [createSubtask] Error al enviar notificación al usuario asignado: {}", ex.getMessage());
                    }
                }
            });
        } else {
            logger.info("[IssueRelationService] [createSubtask] La Subtask no tiene usuario asignado, no se enviará notificación");
        }
        logger.info("[IssueRelationService] [createSubtask] Subtask creada exitosamente con ID={}", subtaskEntity.getId());
        return getIssueDtoResponse(subtaskEntity);
    }

    @Transactional(readOnly = true)
    public List<IssueDtoResponse> getSubtasks(UUID parentId) {
        logger.info("[IssueRelationService] [getSubtasks] Consultando Subtasks del Issue padre con ID={}", parentId);

        List<Issue> subtasks = issueRepository.findByParentId(parentId);

        if (subtasks.isEmpty()) {
            logger.info("[IssueRelationService] [getSubtasks] No se encontraron Subtasks para el Issue padre con ID={}", parentId);
            return Collections.emptyList();
        }

        Set<UUID> userIds = new HashSet<>();
        subtasks.forEach(subtask -> {
            if (subtask.getAssignedId() != null) userIds.add(subtask.getAssignedId());
            if (subtask.getReporterId() != null) userIds.add(subtask.getReporterId());
        });
        logger.debug("[IssueRelationService] [getSubtasks] Se recolectaron {} IDs de usuarios relacionados con las Subtasks", userIds.size());

        List<UserBasicDataDto> usersOpt;
        try {
            usersOpt = userExternalService.getUsersData(
                    JwtContextHolder.getToken(),
                    userIds.stream().map(UUID::toString).toList()
            );
            logger.info("[IssueRelationService] [getSubtasks] Datos de usuarios obtenidos exitosamente ({} usuarios)", usersOpt.size());
        } catch (Exception e) {
            logger.error("[IssueRelationService] [getSubtasks] No se pudieron obtener datos de usuarios: {}", e.getMessage());
            usersOpt = Collections.emptyList();
        }

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        logger.info("[IssueRelationService] [getSubtasks] Finalizando búsqueda de Subtasks correctamente");
        return subtasks.stream()
                .map(issue -> getIssueDtoResponse(userMap, issue))
                .toList();
    }

    private IssueDtoResponse getIssueDtoResponse(Map<UUID, UserBasicDataDto> userMap, Issue issue) {
        IssueDtoResponse issueDtoResponse = issueMapper.toDto(issue);

        issueDtoResponse.setReporterId(userMap.getOrDefault(issue.getReporterId(),
                new UserBasicDataDto(issue.getReporterId(), null, null, null, null, null)));


        issueDtoResponse.setAssignedId(userMap.getOrDefault(issue.getAssignedId(),
                new UserBasicDataDto(issue.getAssignedId(), null, null, null, null, null)));

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

    @Transactional
    public void relateMultipleIssues(UUID sourceId, List<UUID> targetIds) {
        Issue source = issueRepository.findById(sourceId)
                .orElseThrow(() -> new BaseException("Issue origen no encontrado", HttpStatus.NOT_FOUND.value()));

        Set<UUID> alreadyRelatedIds = issueRelationRepository.findBySource_Id(sourceId)
                .stream()
                .map(r -> r.getTarget().getId())
                .collect(Collectors.toSet());

        List<IssueRelation> newRelations = new ArrayList<>();

        for (UUID targetId : targetIds) {
            if (alreadyRelatedIds.contains(targetId)) {
                continue;
            }

            Issue target = issueRepository.findById(targetId)
                    .orElseThrow(() -> new BaseException("Issue destino no encontrado: " + targetId, HttpStatus.NOT_FOUND.value()));

            IssueRelation relation = new IssueRelation();
            relation.setSource(source);
            relation.setTarget(target);
            newRelations.add(relation);
        }

        if (newRelations.isEmpty()) {
            throw new BaseException("No se agregaron relaciones nuevas (ya existentes o inválidas)", HttpStatus.CONFLICT.value());
        }

        issueRelationRepository.saveAll(newRelations);
    }

    @Transactional
    public void unrelateMultipleIssues(UUID sourceId, List<UUID> targetIds) {
        List<IssueRelation> existingRelations = issueRelationRepository.findBySource_Id(sourceId);

        List<IssueRelation> toRemove = existingRelations.stream()
                .filter(r -> targetIds.contains(r.getTarget().getId()))
                .collect(Collectors.toList());

        if (toRemove.isEmpty()) {
            throw new BaseException("No se encontraron relaciones para eliminar", HttpStatus.NOT_FOUND.value());
        }

        issueRelationRepository.deleteAll(toRemove);
    }

    @Transactional
    public List<IssueRelationDto> getRelatedIssues(UUID issueId) {
        logger.info("[IssueRelationService] [getRelatedIssues] Obteniendo issues relacionados de la Issue con ID={}", issueId);
        return issueRelationRepository.findBySource_Id(issueId)
                .stream()
                .map(issueRelationMapper::toDto)
                .toList();
    }

    @Transactional
    public List<IssueRelationDto> getIssuesThatRelateTo(UUID issueId) {
        logger.info("[IssueRelationService] [getIssuesThatRelateTo] Obteniendo issues donde relacionaron a la Issue con ID={}", issueId);
        return issueRelationRepository.findByTarget_Id(issueId)
                .stream()
                .map(issueRelationMapper::toDto)
                .toList();
    }
}
