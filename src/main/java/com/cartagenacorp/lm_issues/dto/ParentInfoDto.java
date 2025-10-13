package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParentInfoDto implements Serializable {
    private UUID id;
    private String title;
}
