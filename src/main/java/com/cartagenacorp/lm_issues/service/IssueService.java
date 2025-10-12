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
import org.springframework.web.server.ResponseStatusException;

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
        if (!issueRepository.existsById(issueId)) {
            throw new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value());
        }

        Description description = descriptionRepository.findById(descriptionId)
                .orElseThrow(() -> new BaseException("Descripción no encontrada", HttpStatus.NOT_FOUND.value()));

        if (!description.getIssue().getId().equals(issueId)) {
            throw new BaseException("La descripción no pertenece a esta Issue", HttpStatus.BAD_REQUEST.value());
        }

        if (files != null && files.length > 0) {
            fileStorageService.saveFiles(description, files);
            logger.info("Archivos adjuntados a la descripción ID={}", descriptionId);
        }
    }

    @Transactional
    public IssueDtoResponse createIssue(IssueDtoRequest issueDtoRequest) {
        if (issueDtoRequest == null) {
            throw new BaseException("La Issue no puede ser nula", HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        if (!projectExternalService.validateProjectExists(issueDtoRequest.getProjectId(), token)) {
            throw new BaseException("El ID del proyecto proporcionado no es válido", HttpStatus.NOT_FOUND.value());
        }

        if (!projectExternalService.validateProjectParticipant(issueDtoRequest.getProjectId(), token)) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        if (issueDtoRequest.getAssignedId() != null &&
                !userExternalService.userExists(issueDtoRequest.getAssignedId(), token)) {
            throw new BaseException("Usuario asignado no encontrado", HttpStatus.NOT_FOUND.value());
        }

        Issue issue = issueMapper.toEntity(issueDtoRequest);
        issue.setReporterId(userId);
        issue.setOrganizationId(organizationId);
        issueRepository.save(issue);
        issueMapper.linkDescriptions(issue);
        Issue savedIssue = issueRepository.save(issue);

        try {
            auditExternalService.logChange(savedIssue.getId(), userId, "CREATE", "Issue created", savedIssue.getProjectId());
        }catch (Exception ignored){}

        if (savedIssue.getAssignedId() != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationExternalService.sendNotification(
                                savedIssue.getAssignedId(),
                                "A new issue has been created to which you are assigned: " + savedIssue.getTitle(),
                                "ISSUE_ASSIGNED",
                                Map.of(
                                        "issueId", savedIssue.getId().toString(),
                                        "projectId", savedIssue.getProjectId().toString()
                                ),
                                savedIssue.getProjectId(),
                                savedIssue.getId()
                        );
                    } catch (Exception ignored) {}
                }
            });
        }

        return getIssueDtoResponse(issue);
    }

    @Transactional
    public List<IssueDTO> createIssuesBatch(List<IssueDTO> issues) {
        UUID userId = JwtContextHolder.getUserId();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        List<Issue> entities = new ArrayList<>();


        for (IssueDTO issueDTO : issues) {
            issueDTO.setReporterId(userId);
            issueDTO.setOrganizationId(organizationId);
           // if (issueDTO.getDescriptionsDTO() == null) { issueDTO.setDescriptionsDTO(new ArrayList<>()); }
            Issue issue = issueMapper.toEntityImport(issueDTO);
            issue.getDescriptions().forEach(description -> description.setIssue(issue));
            entities.add(issue);
        }

        List<Issue> saved = issueRepository.saveAll(entities);
        return saved.stream().map(issueMapper::toDtoImport).toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> findIssues(String keyword, UUID projectId, UUID sprintId, Long status,
                                                        Long priority, Long type, List<UUID> assignedIds,
                                                        Pageable pageable) {

        if (!projectExternalService.validateProjectParticipant(projectId, JwtContextHolder.getToken())) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        Specification<Issue> spec = Specification
                .where(IssueSpecifications.searchByKeyword(keyword))
                .and(IssueSpecifications.hasProject(projectId))
                .and(IssueSpecifications.hasSprint(sprintId))
                .and(IssueSpecifications.hasStatus(status))
                .and(IssueSpecifications.hasPriority(priority))
                .and(IssueSpecifications.hasType(type))
                .and(IssueSpecifications.hasAssignedIn(assignedIds));

        Page<Issue> issues = issueRepository.findAll(spec, pageable);

        Set<UUID> userIds = new HashSet<>();
        issues.getContent().forEach(issue -> {
            if (issue.getAssignedId() != null) userIds.add(issue.getAssignedId());
            if (issue.getReporterId() != null) userIds.add(issue.getReporterId());
        });

        List<UserBasicDataDto> usersOpt = userExternalService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList())
        );

        Map<UUID, UserBasicDataDto> userMap = usersOpt.stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        Page<IssueDtoResponse> mappedPage = issues.map(issue -> getIssueDtoResponse(userMap, issue));
        return new PageResponseDTO<>(mappedPage);
    }

    @Transactional(readOnly = true)
    public IssueDtoResponse getIssueById(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value()));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        return getIssueDtoResponse(issue);
    }

    @Transactional
    public IssueDtoResponse updateIssue(UUID id, IssueDtoRequest updatedIssueDTO) {
        if (updatedIssueDTO == null) {
            throw new BaseException("La Issue no puede ser nula", HttpStatus.BAD_REQUEST.value());
        }

        UUID userId = JwtContextHolder.getUserId();

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value()));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        if(updatedIssueDTO.getProjectId() != null && !updatedIssueDTO.getProjectId().equals(issue.getProjectId())){
            throw new BaseException("El ID del proyecto no puede cambiar", HttpStatus.BAD_REQUEST.value());
        }

        if(updatedIssueDTO.getAssignedId() != null ){
            throw new BaseException("El campo de usuario asignado no es aceptado", HttpStatus.BAD_REQUEST.value());
        }

        List<String> changedFields = new ArrayList<>();
        AtomicBoolean descriptionsChanged = new AtomicBoolean(false);

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
            for (DescriptionDtoRequest descriptionDtoRequest : updatedIssueDTO.getDescriptions()) {
                Description description;

                if (descriptionDtoRequest.getId() != null) {
                    description = issue.getDescriptions().stream()
                            .filter(d -> d.getId().equals(descriptionDtoRequest.getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found"));

                    if (!Objects.equals(description.getText(), descriptionDtoRequest.getText()) ||
                            !Objects.equals(description.getTitle(), descriptionDtoRequest.getTitle())) {
                        description.setTitle(descriptionDtoRequest.getTitle());
                        description.setText(descriptionDtoRequest.getText());
                        descriptionsChanged.set(true);
                    }

                } else {
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
                            fileStorageService.deleteFile(existingFile.getFileUrl());
                            iterator.remove();
                            descriptionsChanged.set(true);
                        }
                    }

                    for (DescriptionFileDtoRequest fileDTO : descriptionDtoRequest.getAttachments()) {
                        if (fileDTO.getId() == null) { // nuevo archivo
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
                descriptionsChanged.set(true);
            }

            if (descriptionsChanged.get()) {
                changedFields.add("descriptions");
                issue.setUpdatedAt(LocalDateTime.now());
            }
        }

        Issue savedIssue = issueRepository.save(issue);

        if (!changedFields.isEmpty()) {
            String auditDesc = "Updated fields: " + String.join(", ", changedFields);
            try {
                auditExternalService.logChange(id, userId, "UPDATE", auditDesc, savedIssue.getProjectId());
            } catch (Exception ignored) {}

            if (savedIssue.getAssignedId() != null) {
                try {
                    notificationExternalService.sendNotification(
                            savedIssue.getAssignedId(),
                            "An issue you are assigned to has been updated: " + savedIssue.getTitle(),
                            "ISSUE_UPDATED",
                            Map.of(
                                    "issueId", savedIssue.getId().toString(),
                                    "projectId", savedIssue.getProjectId().toString()
                            ),
                            savedIssue.getProjectId(),
                            savedIssue.getId()
                    );
                } catch (Exception ignored) {}
            }
        }

        return getIssueDtoResponse(savedIssue);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value()));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        issueRepository.delete(issue);
    }

    @Transactional
    public void deleteIssues(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("La lista de IDs no puede estar vacía");
        }

        ids.forEach(id -> {
            if (!issueRepository.existsById(id)) {
                throw new IllegalArgumentException("No existe un issue con ID: " + id);
            }
        });

        issueRepository.deleteAllById(ids);
    }

    @Transactional
    public IssueDtoResponse assignUserToIssue(UUID issueId, UUID assignedId) {
        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new BaseException("Issue no encontrada", HttpStatus.NOT_FOUND.value()));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new BaseException("No eres participante en este proyecto", HttpStatus.FORBIDDEN.value());
        }

        String auditDescription;
        if (assignedId == null) {
            issue.setAssignedId(null);
            auditDescription = "User unassigned to issue";
        } else {
            if (!userExternalService.userExists(assignedId, token)) {
                throw new BaseException("Usuario asignado no encontrado", HttpStatus.NOT_FOUND.value());
            }
            issue.setAssignedId(assignedId);
            auditDescription = "User assigned to issue";
        }

        Issue savedIssue = issueRepository.save(issue);

        try {
            auditExternalService.logChange(savedIssue.getId(), userId, "ASSIGN", auditDescription, savedIssue.getProjectId());
        } catch (Exception ignored){}

        if (savedIssue.getAssignedId() != null) {
            try {
                notificationExternalService.sendNotification(
                        savedIssue.getAssignedId(),
                        "You have been assigned to an issue: " + savedIssue.getTitle(),
                        "ISSUE_ASSIGNED",
                        Map.of(
                                "issueId", savedIssue.getId().toString(),
                                "projectId", savedIssue.getProjectId().toString()
                        ),
                        savedIssue.getProjectId(),
                        savedIssue.getId()
                );
            } catch (Exception ignored) {}
        }

        return issueMapper.toDto(savedIssue);
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

    @Transactional(readOnly = true)
    public boolean issueExists(UUID id){
        return issueRepository.existsById(id);
    }

    @Transactional
    public void assignIssuesToSprint(List<UUID> issueIds, UUID sprintId) {
        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            throw new BaseException("Algunas Issues no fueron encontradas", HttpStatus.NOT_FOUND.value());
        }

        for (Issue issue : issues) {
            issue.setSprintId(sprintId);
            try {
                UUID userId = JwtContextHolder.getUserId();
                auditExternalService.logChange(issue.getId(), userId, "SPRINT", "Sprint assigned: " + sprintId, issue.getProjectId());
            } catch (Exception ignored){}
        }
        issueRepository.saveAll(issues);
    }

    @Transactional
    public void removeIssuesFromSprint(List<UUID> issueIds) {
        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            throw new BaseException("Algunas Issues no fueron encontradas", HttpStatus.NOT_FOUND.value());
        }

        for (Issue issue : issues) {
            issue.setSprintId(null);
            try {
                UUID userId = JwtContextHolder.getUserId();
                auditExternalService.logChange(issue.getId(), userId, "SPRINT", "Sprint unassigned", issue.getProjectId());
            } catch (Exception ignored){}
        }
        issueRepository.saveAll(issues);
    }
}