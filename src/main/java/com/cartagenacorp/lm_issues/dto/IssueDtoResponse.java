package com.cartagenacorp.lm_issues.dto;

import com.cartagenacorp.lm_issues.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Issue}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDtoResponse implements Serializable {
    UUID id;
    String title;
    List<DescriptionDtoResponse> descriptions;
    Integer estimatedTime;
    UUID projectId;
    UUID sprintId;
    Long priority;
    Long status;
    Long type;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate realDate;
    UserBasicDataDto reporterId;
    UserBasicDataDto assignedId;
    UUID organizationId;
    ParentInfoDto parent;
}