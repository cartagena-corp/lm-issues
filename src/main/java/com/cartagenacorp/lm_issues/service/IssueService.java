package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.*;
import com.cartagenacorp.lm_issues.repository.specifications.IssueSpecifications;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import com.cartagenacorp.lm_issues.util.JwtContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final UserExternalService userExternalService;
    private final ProjectExternalService projectExternalService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public IssueService(IssueRepository issueRepository, IssueMapper issueMapper, UserExternalService userExternalService,
                        ProjectExternalService projectExternalService, AuditService auditService, NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.userExternalService = userExternalService;
        this.projectExternalService = projectExternalService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public IssueDtoResponse createIssue(IssueDtoRequest issueDtoRequest) {
        if (issueDtoRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue cannot be null");
        }

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();
        UUID organizationId = JwtContextHolder.getOrganizationId();

        if (!projectExternalService.validateProjectExists(issueDtoRequest.getProjectId(), token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The project ID provided is not valid");
        }

        if (!projectExternalService.validateProjectParticipant(issueDtoRequest.getProjectId(), token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
        }

        if (issueDtoRequest.getAssignedId() != null &&
                !userExternalService.userExists(issueDtoRequest.getAssignedId(), token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned user not found");
        }

        Issue issue = issueMapper.toEntity(issueDtoRequest);
        issue.setReporterId(userId);
        issue.setOrganizationId(organizationId);
        issueRepository.save(issue);
        issueMapper.linkDescriptions(issue);
        Issue savedIssue = issueRepository.save(issue);

        try {
            auditService.logChange(savedIssue.getId(), userId, "CREATE", "Issue created", savedIssue.getProjectId());
        }catch (Exception ignored){}

        if (savedIssue.getAssignedId() != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        notificationService.sendNotification(
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
        }

        return getIssueDtoResponse(issue);
    }

    @Transactional
    public IssueDtoResponse updateIssue(UUID id, IssueDtoRequest updatedIssueDTO) {
        if (updatedIssueDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue cannot be null");
        }

        UUID userId = JwtContextHolder.getUserId();

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
        }

        if(updatedIssueDTO.getProjectId() != null && !updatedIssueDTO.getProjectId().equals(issue.getProjectId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The project id cannot change" );
        }

        if(updatedIssueDTO.getAssignedId() != null ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The assigned ID is not accepted" );
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
                if (descriptionDtoRequest.getId() != null) {
                    issue.getDescriptions().stream()
                            .filter(description -> description.getId().equals(descriptionDtoRequest.getId()))
                            .findFirst()
                            .ifPresent(description -> {
                                if (!Objects.equals(description.getText(), descriptionDtoRequest.getText()) ||
                                        !Objects.equals(description.getTitle(), descriptionDtoRequest.getTitle())) {
                                    description.setTitle(descriptionDtoRequest.getTitle());
                                    description.setText(descriptionDtoRequest.getText());
                                    descriptionsChanged.set(true);
                                }
                            });
                } else {
                    Description newDescription = new Description();
                    newDescription.setTitle(descriptionDtoRequest.getTitle());
                    newDescription.setText(descriptionDtoRequest.getText());
                    newDescription.setIssue(issue);
                    issue.getDescriptions().add(newDescription);
                    descriptionsChanged.set(true);
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
                auditService.logChange(id, userId, "UPDATE", auditDesc, savedIssue.getProjectId());
            } catch (Exception ignored) {}

            if (savedIssue.getAssignedId() != null) {
                try {
                    notificationService.sendNotification(
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (!projectExternalService.validateProjectParticipant(issue.getProjectId(), JwtContextHolder.getToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this project");
        }

        String auditDescription;
        if (assignedId == null) {
            issue.setAssignedId(null);
            auditDescription = "User unassigned to issue";
        } else {
            if (!userExternalService.userExists(assignedId, token)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            issue.setAssignedId(assignedId);
            auditDescription = "User assigned to issue";
        }

        Issue savedIssue = issueRepository.save(issue);

        try {
            auditService.logChange(savedIssue.getId(), userId, "ASSIGN", auditDescription, savedIssue.getProjectId());
        } catch (Exception ignored){}

        if (savedIssue.getAssignedId() != null) {
            try {
                notificationService.sendNotification(
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some issues were not found");
        }

        for (Issue issue : issues) {
            issue.setSprintId(sprintId);
            try {
                UUID userId = JwtContextHolder.getUserId();
                auditService.logChange(issue.getId(), userId, "SPRINT", "Sprint assigned: " + sprintId, issue.getProjectId());
            } catch (Exception ignored){}
        }
        issueRepository.saveAll(issues);
    }

    @Transactional
    public void removeIssuesFromSprint(List<UUID> issueIds) {
        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some issues were not found");
        }

        for (Issue issue : issues) {
            issue.setSprintId(null);
            try {
                UUID userId = JwtContextHolder.getUserId();
                auditService.logChange(issue.getId(), userId, "SPRINT", "Sprint unassigned", issue.getProjectId());
            } catch (Exception ignored){}
        }
        issueRepository.saveAll(issues);
    }
}