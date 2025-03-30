package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.DescriptionDTO;
import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.enums.IssueEnum.Status;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import com.cartagenacorp.lm_issues.repository.IssueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final UserValidationService userValidationService;

    @Autowired
    public IssueService(IssueRepository issueRepository, IssueMapper issueMapper, UserValidationService userValidationService) {
        this.issueRepository = issueRepository;
        this.issueMapper = issueMapper;
        this.userValidationService = userValidationService;
    }

    @Transactional(readOnly = true)
    public List<IssueDTO> getAllIssues() {
        return issueMapper.issuesToIssueDTOs(issueRepository.findAll());
    }

    @Transactional(readOnly = true)
    public IssueDTO getIssueById(UUID id) {
        return issueMapper.issueToIssueDTO(issueRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Issue not found")));
    }

    @Transactional(readOnly = true)
    public List<IssueDTO> getIssuesByStatus(Status status) {
        return issueMapper.issuesToIssueDTOs(issueRepository.findByStatus(status));
    }

    @Transactional
    public IssueDTO createIssue(IssueDTO issueDTO, String email) {
        if (issueDTO == null) {
            throw new IllegalArgumentException("The issue cannot be null");
        }

        UUID reporterId = userValidationService.authenticateUser(email);
        issueDTO.setReporterId(reporterId);

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
        Issue issue = issueRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        issue.setTitle(updatedIssueDTO.getTitle());
        issue.setEstimatedTime(updatedIssueDTO.getEstimatedTime());
        issue.setPriority(updatedIssueDTO.getPriority());
        issue.setStatus(updatedIssueDTO.getStatus());

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
        return issueMapper.issueToIssueDTO(savedIssue);
    }

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));
        issueRepository.delete(issue);
    }

    @Transactional
    public IssueDTO reopenIssue(UUID id) {
        return issueRepository.findById(id)
                .map(issue -> {
                    if ("RESOLVED".equalsIgnoreCase(issue.getStatus().toString()) || "CLOSED".equalsIgnoreCase(issue.getStatus().toString())) {
                        issue.setStatus(Status.REOPEN);
                        Issue savedIssue = issueRepository.save(issue);
                        return issueMapper.issueToIssueDTO(savedIssue);
                    }
                    throw new IllegalStateException("Issue is not in a closed state and cannot be reopened.");
                })
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));
    }

    @Transactional
    public IssueDTO assignUserToIssue(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (userId == null) {
            issue.setAssignedId(null);
        } else {
            if (!userValidationService.userExists(userId)) {
                throw new EntityNotFoundException("User not found");
            }
            issue.setAssignedId(userId);
        }

        Issue savedIssue = issueRepository.save(issue);
        return issueMapper.issueToIssueDTO(savedIssue);
    }
}