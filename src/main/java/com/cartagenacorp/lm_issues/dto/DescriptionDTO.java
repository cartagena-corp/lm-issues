package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionDTO {
    private UUID id;
    private String text;
    private UUID issueId;
}
