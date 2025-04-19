package com.cartagenacorp.lm_issues.controller;

import com.cartagenacorp.lm_issues.dto.*;
import com.cartagenacorp.lm_issues.service.IssueService;
import com.cartagenacorp.lm_issues.util.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService issueService;

    @Autowired
    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDtoResponse>> getAllIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDTO<IssueDtoResponse> issues = issueService.getAllIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/status/")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDtoResponse>> getIssuesByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDTO<IssueDtoResponse> issues = issueService.getIssuesByStatus(status, pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<?> getIssueById(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse issue = issueService.getIssueById(uuid);
        return ResponseEntity.ok(issue);
    }

    @GetMapping("/project/{projectId}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<?> getIssuesByProjectId(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        UUID uuid = UUID.fromString(projectId);

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDTO<IssueDtoResponse> issues = issueService.getIssuesByProjectId(uuid, pageable);
        return ResponseEntity.ok(issues);
    }

    @PostMapping
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> createIssue(@RequestBody @Valid IssueDtoRequest issueDtoRequest) {
        IssueDtoResponse createdIssue = issueService.createIssue(issueDtoRequest);
        return new ResponseEntity<>(createdIssue, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_UPDATE"})
    public ResponseEntity<?> updateIssue(@PathVariable String id, @RequestBody @Valid IssueDtoRequest issueDtoRequest) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse updatedIssue = issueService.updateIssue(uuid, issueDtoRequest);
        return ResponseEntity.ok(updatedIssue);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> deleteIssue(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        issueService.deleteIssue(uuid);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reopen/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_UPDATE"})
    public ResponseEntity<?> reopenIssue(@PathVariable String id, @RequestParam Long newStatus) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse reopenedIssue = issueService.reopenIssue(uuid, newStatus);
        return ResponseEntity.ok(reopenedIssue);
    }

    @PatchMapping("/assignUser/{id}")
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> assignUsersToIssue(@PathVariable String id, @RequestBody(required = false) UUID userId) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse updatedIssue = issueService.assignUserToIssue(uuid, userId);
        return ResponseEntity.ok(updatedIssue);
    }

    @GetMapping("/search")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDtoResponse>> searchIssues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String sprintId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String assignedId,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID assignedIdUuid = parseUUIDParam(assignedId);
        UUID projectIdUuid = parseUUIDParam(projectId);
        UUID sprintIdUuid = parseUUIDParam(sprintId);
        Long statusParsed = parseLongParam(status);
        Long priorityParsed = parseLongParam(priority);
        Long typeParsed = parseLongParam(type);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponseDTO<IssueDtoResponse> results = issueService.findIssues(
                keyword, projectIdUuid, sprintIdUuid, statusParsed, priorityParsed, typeParsed, assignedIdUuid, pageable);

        return ResponseEntity.ok(results);
    }

    private Long parseLongParam(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.equalsIgnoreCase("null")) return -1L;
        return Long.valueOf(value);
    }

    private UUID parseUUIDParam(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.equalsIgnoreCase("null")) return new UUID(0L, 0L);
        return UUID.fromString(value);
    }

    @GetMapping("/validate/{id}")
    public ResponseEntity<Boolean> issueExists(@PathVariable String id){
        UUID uuid = UUID.fromString(id);
        return ResponseEntity.status(HttpStatus.OK).body(issueService.issueExists(uuid));
    }

    @PostMapping("/batch")
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> createIssuesBatch(@RequestBody List<IssueDTO> issues) {
        List<IssueDTO> result = issueService.createIssuesBatch(issues);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/assign")
    @RequiresPermission({"SPRINT_CRUD"})
    public ResponseEntity<Void> assignIssuesToSprint(@RequestBody AssignRequest request) {
        issueService.assignIssuesToSprint(request.getIssueIds(), request.getSprintId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/remove")
    @RequiresPermission({"SPRINT_CRUD"})
    public ResponseEntity<Void> removeIssuesFromSprint(@RequestBody RemoveRequest request) {
        issueService.removeIssuesFromSprint(request.getIssueIds());
        return ResponseEntity.ok().build();
    }
}