package com.cartagenacorp.lm_issues.controller;

import com.cartagenacorp.lm_issues.dto.*;
import com.cartagenacorp.lm_issues.service.IssueService;
import com.cartagenacorp.lm_issues.util.RequiresPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @RequiresPermission({"ISSUE_CREATE"})
    public ResponseEntity<?> createIssue(@RequestBody @Valid IssueDtoRequest issueDtoRequest) {
        IssueDtoResponse createdIssue = issueService.createIssue(issueDtoRequest);
        return new ResponseEntity<>(createdIssue, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    @RequiresPermission({"ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDtoResponse>> searchIssues(
            @RequestParam(required = false) String keyword,
            @RequestParam @NotBlank String projectId,
            @RequestParam(required = false) String sprintId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> assignedIds,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<UUID> assignedIdUuids = assignedIds != null
                ? assignedIds.stream().map(UUID::fromString).toList()
                : Collections.emptyList();
        UUID projectIdUuid = parseUUIDParam(projectId);
        UUID sprintIdUuid = parseUUIDParam(sprintId);
        Long statusParsed = parseLongParam(status);
        Long priorityParsed = parseLongParam(priority);
        Long typeParsed = parseLongParam(type);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, sortBy).and(Sort.by(sortDirection, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDTO<IssueDtoResponse> results = issueService.findIssues(
                keyword, projectIdUuid, sprintIdUuid, statusParsed, priorityParsed, typeParsed, assignedIdUuids, pageable);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @RequiresPermission({"ISSUE_READ"})
    public ResponseEntity<?> getIssueById(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse issue = issueService.getIssueById(uuid);
        return ResponseEntity.ok(issue);
    }

    @PutMapping("/{id}")
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<?> updateIssue(@PathVariable String id, @RequestBody @Valid IssueDtoRequest issueDtoRequest) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse updatedIssue = issueService.updateIssue(uuid, issueDtoRequest);
        return ResponseEntity.ok(updatedIssue);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission({"ISSUE_DELETE"})
    public ResponseEntity<?> deleteIssue(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        issueService.deleteIssue(uuid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    @RequiresPermission({"ISSUE_DELETE"})
    public ResponseEntity<Void> deleteIssues(@RequestBody List<UUID> ids) {
        issueService.deleteIssues(ids);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/assignUser/{id}")
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<?> assignUsersToIssue(@PathVariable String id, @RequestBody(required = false) UUID userId) {
        UUID uuid = UUID.fromString(id);
        IssueDtoResponse updatedIssue = issueService.assignUserToIssue(uuid, userId);
        return ResponseEntity.ok(updatedIssue);
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

    @GetMapping("/validate/{id}")  //se usa desde lm-comments (uso interno)
    @RequiresPermission({"COMMENT_CREATE"})
    public ResponseEntity<Boolean> issueExists(@PathVariable String id){
        UUID uuid = UUID.fromString(id);
        return ResponseEntity.status(HttpStatus.OK).body(issueService.issueExists(uuid));
    }

    @PostMapping("/batch")  //se usa desde lm-integrations (uso interno) o desde frontend para crear varias tareas
    @RequiresPermission({"ISSUE_CREATE" , "IMPORT_PROJECT"})
    public ResponseEntity<?> createIssuesBatch(@RequestBody List<IssueDTO> issues) {
        List<IssueDTO> result = issueService.createIssuesBatch(issues);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/assign")  //se usa desde lm-sprints (uso interno)
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<Void> assignIssuesToSprint(@RequestBody AssignRequest request) {
        issueService.assignIssuesToSprint(request.getIssueIds(), request.getSprintId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/remove") //se usa desde lm-sprints (uso interno)
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<Void> removeIssuesFromSprint(@RequestBody RemoveRequest request) {
        issueService.removeIssuesFromSprint(request.getIssueIds());
        return ResponseEntity.ok().build();
    }
}