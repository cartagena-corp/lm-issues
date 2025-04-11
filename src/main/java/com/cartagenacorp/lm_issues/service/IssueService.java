package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.DescriptionDTO;
import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.dto.PageResponseDTO;
import com.cartagenacorp.lm_issues.repository.specifications.IssueSpecifications;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.enums.IssueEnum;
import com.cartagenacorp.lm_issues.enums.IssueEnum.Status;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final UserValidationService userValidationService;
    private final ProjectValidationService projectValidationService;
    private final AuditService auditService;

    @Autowired
    public IssueService(IssueRepository issueRepository, IssueMapper issueMapper, UserValidationService userValidationService,
                        ProjectValidationService projectValidationService, AuditService auditService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.userValidationService = userValidationService;
        this.projectValidationService = projectValidationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDTO> getAllIssues(Pageable pageable) {
        Page<Issue> issues = issueRepository.findAll(pageable);
        Page<IssueDTO> issueDTOs = issues.map(issueMapper::issueToIssueDTO);
        return new PageResponseDTO<>(issueDTOs);
    }

    @Transactional(readOnly = true)
    public IssueDTO getIssueById(UUID id) {
        return issueMapper.issueToIssueDTO(issueRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found")));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDTO> getIssuesByStatus(Status status, Pageable pageable) {
        Page<Issue> issues = issueRepository.findByStatus(status, pageable);
        Page<IssueDTO> issueDTOs = issues.map(issueMapper::issueToIssueDTO);
        return new PageResponseDTO<>(issueDTOs);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDTO> getIssuesByProjectId(UUID projectId, Pageable pageable) {
        Page<Issue> issues = issueRepository.findByProjectId(projectId, pageable);
        Page<IssueDTO> issueDTOs = issues.map(issueMapper::issueToIssueDTO);
        return new PageResponseDTO<>(issueDTOs);
    }

    @Transactional
    public IssueDTO createIssue(IssueDTO issueDTO) {
        if (issueDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue cannot be null");
        }

        UUID userId = JwtContextHolder.getUserId();

        if (!projectValidationService.validateProjectExists(issueDTO.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The project ID provided is not valid");
        }

        issueDTO.setReporterId(userId);
        if(issueDTO.getStatus() == null){
            issueDTO.setStatus(Status.OPEN);
        }

        Issue issue = issueMapper.issueDTOToIssue(issueDTO);
        issueRepository.save(issue);
        issue.getDescriptions().forEach(description -> description.setIssue(issue));
        Issue savedIssue = issueRepository.save(issue);
        issueRepository.flush();
        return issueMapper.issueToIssueDTO(savedIssue);
    }

    @Transactional
    public IssueDTO updateIssue(UUID id, IssueDTO updatedIssueDTO) {
        if (updatedIssueDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The issue cannot be null");
        }

        UUID userId = JwtContextHolder.getUserId();

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        List<String> changedFields = new ArrayList<>();
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

        if (updatedIssueDTO.getSprintId() != null) {
            issue.setSprintId(updatedIssueDTO.getSprintId());
        } else {
            issue.setSprintId(null);
        }

        if (updatedIssueDTO.getDescriptionsDTO() != null) {
            for (DescriptionDTO descriptionDTO : updatedIssueDTO.getDescriptionsDTO()) {
                if (descriptionDTO.getId() != null) {
                    issue.getDescriptions().stream()
                            .filter(description -> description.getId().equals(descriptionDTO.getId()))
                            .findFirst()
                            .ifPresent(description -> description.setText(descriptionDTO.getText()));
                } else {
                    Description newDescription = new Description();
                    newDescription.setText(descriptionDTO.getText());
                    newDescription.setIssue(issue);
                    issue.getDescriptions().add(newDescription);
                }
            }
            issue.getDescriptions().removeIf(
                    description -> description.getId() != null && updatedIssueDTO.getDescriptionsDTO().stream()
                            .noneMatch(descriptionDTO ->
                                    descriptionDTO.getId() != null && descriptionDTO.getId().equals(description.getId())
                            )
            );
        }
        Issue savedIssue = issueRepository.save(issue);

        if (!changedFields.isEmpty()) {
            String auditDesc = "Updated fields: " + String.join(", ", changedFields);
            try {
                auditService.logChange(id, userId, "UPDATE", auditDesc, savedIssue.getProjectId());
            } catch (Exception ignored) {}
        }

        return issueMapper.issueToIssueDTO(savedIssue);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
        issueRepository.delete(issue);
    }

    @Transactional
    public IssueDTO reopenIssue(UUID id) {
        UUID userId = JwtContextHolder.getUserId();

        return issueRepository.findById(id)
                .map(issue -> {
                    if ("RESOLVED".equalsIgnoreCase(issue.getStatus().toString()) || "CLOSED".equalsIgnoreCase(issue.getStatus().toString())) {
                        issue.setStatus(Status.REOPEN);
                        Issue savedIssue = issueRepository.save(issue);
                        try {
                            auditService.logChange(id, userId, "UPDATE", "Issue reopened", savedIssue.getProjectId());
                        }catch (Exception ignored){}
                        return issueMapper.issueToIssueDTO(savedIssue);
                    }
                    throw new IllegalStateException("Issue is not in a closed state and cannot be reopened.");
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
    }

    @Transactional
    public IssueDTO assignUserToIssue(UUID issueId, UUID assignedId) {
        UUID userId = JwtContextHolder.getUserId();
        String token = JwtContextHolder.getToken();

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));

        String auditDescription = "";
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
        }catch (Exception ignored){}

        return issueMapper.issueToIssueDTO(savedIssue);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<IssueDTO> findIssues(String keyword, UUID projectId, Status status,
                                     IssueEnum.Priority priority, UUID assignedId,
                                     Pageable pageable) {

        Specification<Issue> spec = Specification
                .where(IssueSpecifications.searchByKeyword(keyword))
                .and(IssueSpecifications.hasProject(projectId))
                .and(IssueSpecifications.hasStatus(status))
                .and(IssueSpecifications.hasPriority(priority))
                .and(IssueSpecifications.hasAssigned(assignedId));

        Page<Issue> issues = issueRepository.findAll(spec, pageable);
        Page<IssueDTO> issueDTOs = issues.map(issueMapper::issueToIssueDTO);

        return new PageResponseDTO<>(issueDTOs);
    }

    @Transactional(readOnly = true)
    public boolean issueExists(UUID id){
        return issueRepository.existsById(id);
    }
}