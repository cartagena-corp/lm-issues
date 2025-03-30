package com.cartagenacorp.lm_issues.controller;

import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.enums.IssueEnum;
import com.cartagenacorp.lm_issues.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*")
public class IssueController {
    private final IssueService issueService;

    @Autowired
    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    public ResponseEntity<List<IssueDTO>> getAllIssues() {
        List<IssueDTO> issues = issueService.getAllIssues();
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/status/")
    public ResponseEntity<List<IssueDTO>> getIssuesByStatus(@RequestParam String status) {
        IssueEnum.Status statusEnum = IssueEnum.Status.valueOf(status.toUpperCase());
        List<IssueDTO> issues = issueService.getIssuesByStatus(statusEnum);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueDTO> getIssueById(@PathVariable UUID id) {
        IssueDTO issue = issueService.getIssueById(id);
        return ResponseEntity.ok(issue);
    }

    @PostMapping
    public ResponseEntity<IssueDTO> createIssue(@RequestBody IssueDTO issueDTO, @RequestParam String email) {
        IssueDTO createdIssue = issueService.createIssue(issueDTO, email);
        return new ResponseEntity<>(createdIssue, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IssueDTO> updateIssue(@PathVariable UUID id, @RequestBody IssueDTO issueDTO) {
        IssueDTO updatedIssue = issueService.updateIssue(id, issueDTO);
        return ResponseEntity.ok(updatedIssue);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable UUID id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reopen/{id}")
    public ResponseEntity<IssueDTO> reopenIssue(@PathVariable UUID id) {
        IssueDTO reopenedIssue = issueService.reopenIssue(id);
        return ResponseEntity.ok(reopenedIssue);
    }

    @PatchMapping("/assignUser/{id}")
    public ResponseEntity<IssueDTO> assignUsersToIssue(@PathVariable UUID id, @RequestBody(required = false) UUID userId) {
        IssueDTO updatedIssue = issueService.assignUserToIssue(id, userId);
        return ResponseEntity.ok(updatedIssue);
    }
}