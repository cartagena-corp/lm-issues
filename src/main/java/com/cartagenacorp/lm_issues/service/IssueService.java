package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.repository.IssueRepository;
import org.springframework.stereotype.Service;

import com.cartagenacorp.lm_issues.entity.Issue;
import java.util.List;
import java.util.Optional;

@Service
public class IssueService {

    private final IssueRepository issueRepository;

    public IssueService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public Optional<Issue> getIssueById(Long id) {
        return issueRepository.findById(id);
    }

    public Issue createIssue(Issue issue) {
        return issueRepository.save(issue);
    }

    public Issue updateIssue(Long id, Issue issueDetails) {
        return issueRepository.findById(id).map(issue -> {
            issue.setTitle(issueDetails.getTitle());
            issue.setDescription(issueDetails.getDescription());
            issue.setStatus(issueDetails.getStatus());
            return issueRepository.save(issue);
        }).orElseThrow(() -> new RuntimeException("Issue not found"));
    }

    public void deleteIssue(Long id) {
        issueRepository.deleteById(id);
    }
}
