package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.cartagenacorp.lm_issues.entity.DescriptionFile}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionFileDtoRequest implements Serializable {
    private UUID id;
    private String fileName;
    private String fileUrl;
}