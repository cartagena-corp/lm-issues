package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {
    private UUID id;
    private String title;
    private List<DescriptionDTO> descriptionsDTO;
    private Integer estimatedTime;
    private UUID projectId;
    private UUID sprintId;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID reporterId;
    private UUID assignedId;
}
