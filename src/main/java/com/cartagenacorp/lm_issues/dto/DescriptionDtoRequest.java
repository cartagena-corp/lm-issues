package com.cartagenacorp.lm_issues.dto;

import com.cartagenacorp.lm_issues.entity.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link Description}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionDtoRequest implements Serializable {
    private UUID id;
    private String title;
    private String text;
}