package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueHistoryDto {
    private UUID issueId;
    private String issueTitle;
    private UUID userId;
    private String action;
    private String description;
    private UUID projectId;
    private String beforeChange;
    private String afterChange;
}
