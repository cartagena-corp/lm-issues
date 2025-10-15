package com.cartagenacorp.lm_issues.controller;

import com.cartagenacorp.lm_issues.dto.IssueDtoRequest;
import com.cartagenacorp.lm_issues.dto.IssueDtoResponse;
import com.cartagenacorp.lm_issues.dto.IssueRelationDto;
import com.cartagenacorp.lm_issues.dto.NotificationResponse;
import com.cartagenacorp.lm_issues.service.IssueRelationService;
import com.cartagenacorp.lm_issues.util.RequiresPermission;
import com.cartagenacorp.lm_issues.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/relations")
public class IssueRelationController {

    private final IssueRelationService issueRelationService;

    public IssueRelationController(IssueRelationService issueRelationService) {
        this.issueRelationService = issueRelationService;
    }

    @PostMapping("/{parentId}/subtasks")
    @RequiresPermission({"ISSUE_CREATE", "ISSUE_UPDATE"})
    public ResponseEntity<IssueDtoResponse> createSubtask(
            @PathVariable UUID parentId,
            @RequestBody IssueDtoRequest subtask
    ) {
        IssueDtoResponse subtaskCreated = issueRelationService.createSubtask(parentId, subtask);
        return ResponseEntity.ok(subtaskCreated);
    }

    @GetMapping("/{parentId}/subtasks")
    @RequiresPermission({"ISSUE_READ"})
    public ResponseEntity<List<IssueDtoResponse>> getSubtasks(@PathVariable UUID parentId) {
        return ResponseEntity.ok(issueRelationService.getSubtasks(parentId));
    }

    @PostMapping("/{sourceId}/related/{targetId}")
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<NotificationResponse> relateIssues(@PathVariable UUID sourceId, @PathVariable UUID targetId) {
        issueRelationService.relateIssues(sourceId, targetId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success("Issues vinculadas correctamente"));
    }

    @DeleteMapping("/{sourceId}/unrelate/{targetId}")
    @RequiresPermission({"ISSUE_UPDATE"})
    public ResponseEntity<NotificationResponse> unrelateIssues(@PathVariable UUID sourceId, @PathVariable UUID targetId) {
        issueRelationService.unrelateIssues(sourceId, targetId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success("Issues desvinculadas correctamente"));
    }

    @GetMapping("/{issueId}/related")
    @RequiresPermission({"ISSUE_READ"})
    public ResponseEntity<List<IssueRelationDto>> getRelatedIssues(@PathVariable UUID issueId) {
        return ResponseEntity.ok(issueRelationService.getRelatedIssues(issueId));
    }

    @GetMapping("/{issueId}/related-to")
    @RequiresPermission({"ISSUE_READ"})
    public ResponseEntity<List<IssueRelationDto>> getIssuesThatRelateTo(@PathVariable UUID issueId) {
        return ResponseEntity.ok(issueRelationService.getIssuesThatRelateTo(issueId));
    }
}
