package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueRelationDto {
    private Long id;
    private UUID targetId;
    private String targetTitle;
    private Long type;
    private Long status;
}
