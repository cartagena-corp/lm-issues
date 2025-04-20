package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.*;
import com.cartagenacorp.lm_issues.repository.specifications.IssueSpecifications;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import com.cartagenacorp.lm_issues.util.JwtContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserValidationService userValidationService;
    private final ProjectValidationService projectValidationService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Autowired
    public IssueService(IssueRepository issueRepository, IssueMapper issueMapper, UserValidationService userValidationService,
                        ProjectValidationService projectValidationService, AuditService auditService, NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.userValidationService = userValidationService;
        this.projectValidationService = projectValidationService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> getAllIssues(Pageable pageable) {
        Page<Issue> issues = issueRepository.findAll(pageable);
        return new PageResponseDTO<>(issues.map(issueMapper::toDto));
    }

    @Transactional(readOnly = true)
    public IssueDtoResponse getIssueById(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        return getIssueDtoResponse(issue);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> getIssuesByStatus(String status, Pageable pageable) {
        Page<Issue> issues = issueRepository.findByStatus(status, pageable);
        return new PageResponseDTO<>(issues.map(issueMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> getIssuesByProjectId(UUID projectId, Pageable pageable) {
        Page<Issue> issues = issueRepository.findByProjectId(projectId, pageable);
        return new PageResponseDTO<>(issues.map(issueMapper::toDto));
    }

    @Transactional
    public IssueDtoResponse createIssue(IssueDtoRequest issueDtoRequest) {
        if (issueDtoRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue cannot be null");
        }

        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();

        if (!projectValidationService.validateProjectExists(issueDtoRequest.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The project ID provided is not valid");
        }

        if (issueDtoRequest.getAssignedId() != null &&
                !userValidationService.userExists(issueDtoRequest.getAssignedId(), token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Issue issue = issueMapper.toEntity(issueDtoRequest);
        issue.setReporterId(userId);
        issueRepository.save(issue);
        issueMapper.linkDescriptions(issue);
        Issue savedIssue = issueRepository.save(issue);

        if (savedIssue.getAssignedId() != null) {
            try {
                notificationService.sendNotification(
                        savedIssue.getAssignedId(),
                        "A new issue has been created to which you are assigned: " + savedIssue.getTitle(),
                        "ISSUE_ASSIGNED",
                        Map.of(
                                "issueId", savedIssue.getId().toString(),
                                "projectId", savedIssue.getProjectId().toString()
                        )
                );
            } catch (Exception ignored) {}
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

        if (updatedIssueDTO.getDescriptions() != null) {
            for (DescriptionDtoRequest descriptionDtoRequest : updatedIssueDTO.getDescriptions()) {
                if (descriptionDtoRequest.getId() != null) {
                    issue.getDescriptions().stream()
                            .filter(description -> description.getId().equals(descriptionDtoRequest.getId()))
                            .findFirst()
                            .ifPresent(description -> {
                                if (!Objects.equals(description.getText(), descriptionDtoRequest.getText())) {
                                    description.setText(descriptionDtoRequest.getText());
                                    descriptionsChanged.set(true);
                                }
                            });
                } else {
                    Description newDescription = new Description();
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
                            )
                    );
                } catch (Exception ignored) {}
            }
        }

        return issueMapper.toDto(savedIssue);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
        issueRepository.delete(issue);
    }

    @Transactional
    public IssueDtoResponse reopenIssue(UUID id, Long newStatus) {
        UUID userId = JwtContextHolder.getUserId();

        return issueRepository.findById(id)
                .map(issue -> {
                    issue.setStatus(newStatus);
                    Issue savedIssue = issueRepository.save(issue);
                    try {
                        auditService.logChange(id, userId, "UPDATE", "Issue reopened", savedIssue.getProjectId());
                    }catch (Exception ignored){}
                    return issueMapper.toDto(savedIssue);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
    }

    @Transactional
    public IssueDtoResponse assignUserToIssue(UUID issueId, UUID assignedId) {
        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        String auditDescription;
        if (assignedId == null) {
            issue.setAssignedId(null);
            auditDescription = "User unassigned to issue";
        } else {
            if (!userValidationService.userExists(assignedId, token)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            issue.setAssignedId(assignedId);
            auditDescription = "User assigned to issue";
        }

        Issue savedIssue = issueRepository.save(issue);

        try {
            auditService.logChange(issueId, userId, "UPDATE", auditDescription, savedIssue.getProjectId());
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
                        )
                );
            } catch (Exception ignored) {}
        }

        return issueMapper.toDto(savedIssue);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDtoResponse> findIssues(String keyword, UUID projectId, UUID sprintId, Long status,
                                                Long priority, Long type, UUID assignedId,
                                     Pageable pageable) {

        Specification<Issue> spec = Specification
                .where(IssueSpecifications.searchByKeyword(keyword))
                .and(IssueSpecifications.hasProject(projectId))
                .and(IssueSpecifications.hasSprint(sprintId))
                .and(IssueSpecifications.hasStatus(status))
                .and(IssueSpecifications.hasPriority(priority))
                .and(IssueSpecifications.hasType(type))
                .and(IssueSpecifications.hasAssigned(assignedId));

        Page<Issue> issues = issueRepository.findAll(spec, pageable);

        Set<UUID> userIds = new HashSet<>();
        issues.getContent().forEach(issue -> {
            if (issue.getAssignedId() != null) userIds.add(issue.getAssignedId());
            if (issue.getReporterId() != null) userIds.add(issue.getReporterId());
        });

        Optional<List<UserBasicDataDto>> usersOpt = userValidationService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream().map(UUID::toString).collect(Collectors.toList())
        );

        Map<UUID, UserBasicDataDto> userMap = usersOpt
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        Page<IssueDtoResponse> mappedPage = issues.map(issue -> getIssueDtoResponse(userMap, issue));
        return new PageResponseDTO<>(mappedPage);
    }

    private IssueDtoResponse getIssueDtoResponse(Map<UUID, UserBasicDataDto> userMap, Issue issue) {
        IssueDtoResponse issueDtoResponse = issueMapper.toDto(issue);

        issueDtoResponse.setReporterId(userMap.getOrDefault(issue.getReporterId(),
                new UserBasicDataDto(issue.getReporterId(), null, null, null)));


        issueDtoResponse.setAssignedId(userMap.getOrDefault(issue.getAssignedId(),
                new UserBasicDataDto(issue.getAssignedId(), null, null, null)));

        return issueDtoResponse;
    }

    private IssueDtoResponse getIssueDtoResponse(Issue issue) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(issue.getReporterId());
        if (issue.getAssignedId() != null) { userIds.add(issue.getAssignedId());}

        Optional<List<UserBasicDataDto>> usersOpt = userValidationService.getUsersData(
                JwtContextHolder.getToken(),
                userIds.stream().map(UUID::toString).toList()
        );

        Map<UUID, UserBasicDataDto> userMap = usersOpt
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(UserBasicDataDto::getId, Function.identity()));

        return getIssueDtoResponse(userMap, issue);
    }

    @Transactional(readOnly = true)
    public boolean issueExists(UUID id){
        return issueRepository.existsById(id);
    }

    @Transactional
    public List<IssueDTO> createIssuesBatch(List<IssueDTO> issues) {
        UUID userId = JwtContextHolder.getUserId();
        List<Issue> entities = new ArrayList<>();

        for (IssueDTO issueDTO : issues) {
            issueDTO.setReporterId(userId);

            Issue issue = issueMapper.toEntityImport(issueDTO);
            issue.getDescriptions().forEach(description -> description.setIssue(issue));
            entities.add(issue);
        }

        List<Issue> saved = issueRepository.saveAll(entities);
        return saved.stream().map(issueMapper::toDtoImport).toList();
    }

    @Transactional
    public void assignIssuesToSprint(List<UUID> issueIds, UUID sprintId) {
        List<Issue> issues = issueRepository.findAllById(issueIds);

        if (issues.size() != issueIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some issues were not found");
        }

        for (Issue issue : issues) {
            issue.setSprintId(sprintId);
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
        }
        issueRepository.saveAll(issues);
    }
}