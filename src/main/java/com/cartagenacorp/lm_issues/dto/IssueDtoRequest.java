package com.cartagenacorp.lm_issues.dto;

import com.cartagenacorp.lm_issues.entity.Issue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Issue}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDtoRequest implements Serializable {
    @NotBlank(message = "Title is required")
    private String title;

    private List<DescriptionDtoRequest> descriptions = new ArrayList<>();

    @NotNull(message = "Estimated time is required")
    @Min(value = 0, message = "Estimated time must be zero or greater")
    private Integer estimatedTime;

    private UUID projectId;

    @NotNull(message = "Priority is required")
    private Long priority;

    @NotNull(message = "Status is required")
    private Long status;

    @NotNull(message = "Type is required")
    private Long type;

    private UUID assignedId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate realDate;
}