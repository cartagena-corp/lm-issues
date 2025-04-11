package com.cartagenacorp.lm_issues.controller;

import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.dto.PageResponseDTO;
import com.cartagenacorp.lm_issues.enums.IssueEnum;
import com.cartagenacorp.lm_issues.service.IssueService;
import com.cartagenacorp.lm_issues.util.RequiresPermission;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDTO>> getAllIssues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponseDTO<IssueDTO> issues = issueService.getAllIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/status/")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDTO>> getIssuesByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        IssueEnum.Status statusEnum = IssueEnum.Status.valueOf(status.toUpperCase());
        PageResponseDTO<IssueDTO> issues = issueService.getIssuesByStatus(statusEnum, pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<?> getIssueById(@PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            IssueDTO issue = issueService.getIssueById(uuid);
            return ResponseEntity.ok(issue);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/project/{projectId}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<?> getIssuesByProjectId(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        try {
            UUID uuid = UUID.fromString(projectId);

            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            PageResponseDTO<IssueDTO> issues = issueService.getIssuesByProjectId(uuid, pageable);
            return ResponseEntity.ok(issues);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> createIssue(@RequestBody IssueDTO issueDTO) {
        try {
            IssueDTO createdIssue = issueService.createIssue(issueDTO);
            return new ResponseEntity<>(createdIssue, HttpStatus.CREATED);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred" + ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_UPDATE"})
    public ResponseEntity<?> updateIssue(@PathVariable String id, @RequestBody IssueDTO issueDTO) {
        try {
            UUID uuid = UUID.fromString(id);
            IssueDTO updatedIssue = issueService.updateIssue(uuid, issueDTO);
            return ResponseEntity.ok(updatedIssue);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> deleteIssue(@PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            issueService.deleteIssue(uuid);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @PatchMapping("/reopen/{id}")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_UPDATE"})
    public ResponseEntity<?> reopenIssue(@PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            IssueDTO reopenedIssue = issueService.reopenIssue(uuid);
            return ResponseEntity.ok(reopenedIssue);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @PatchMapping("/assignUser/{id}")
    @RequiresPermission({"ISSUE_CRUD"})
    public ResponseEntity<?> assignUsersToIssue(@PathVariable String id, @RequestBody(required = false) UUID userId) {
        try {
            UUID uuid = UUID.fromString(id);
            IssueDTO updatedIssue = issueService.assignUserToIssue(uuid, userId);
            return ResponseEntity.ok(updatedIssue);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @GetMapping("/search")
    @RequiresPermission({"ISSUE_CRUD", "ISSUE_READ"})
    public ResponseEntity<PageResponseDTO<IssueDTO>> searchIssues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assignedId,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        IssueEnum.Status statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = IssueEnum.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }

        IssueEnum.Priority priorityEnum = null;
        if (priority != null && !priority.isEmpty()) {
            try {
                priorityEnum = IssueEnum.Priority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponseDTO<IssueDTO> results = issueService.findIssues(
                keyword, projectId, statusEnum, priorityEnum, assignedId, pageable);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/validate/{id}")
    public ResponseEntity<Boolean> issueExists(@PathVariable String id){
        try {
            UUID uuid = UUID.fromString(id);
            return ResponseEntity.status(HttpStatus.OK).body(issueService.issueExists(uuid));
        }  catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}