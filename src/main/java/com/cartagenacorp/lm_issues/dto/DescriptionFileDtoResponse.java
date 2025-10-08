package com.cartagenacorp.lm_issues.dto;

import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import lombok.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link DescriptionFile}
 */
@Value
public class DescriptionFileDtoResponse implements Serializable {
    UUID id;
    String fileName;
    String fileUrl;
}