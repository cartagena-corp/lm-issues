package com.cartagenacorp.lm_issues.dto;

import com.cartagenacorp.lm_issues.entity.Description;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Description}
 */
@Value
public class DescriptionDtoResponse implements Serializable {
    UUID id;
    String title;
    String text;
    List<DescriptionFileDtoResponse> attachments;
}