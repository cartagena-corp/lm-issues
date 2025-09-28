package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.DescriptionDtoRequest;
import com.cartagenacorp.lm_issues.dto.DescriptionDtoResponse;
import com.cartagenacorp.lm_issues.dto.DescriptionFileDtoRequest;
import com.cartagenacorp.lm_issues.dto.DescriptionFileDtoResponse;
import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DescriptionMapper {
    Description toEntity(DescriptionDtoRequest descriptionDtoRequest);

    DescriptionDtoResponse toDto(Description description);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Description partialUpdate(DescriptionDtoRequest descriptionDtoRequest, @MappingTarget Description description);


    // ---------- DescriptionFile ----------
    DescriptionFile toEntity(DescriptionFileDtoRequest dto);

    DescriptionFileDtoResponse toDto(DescriptionFile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DescriptionFile partialUpdate(
            DescriptionFileDtoRequest dto,
            @MappingTarget DescriptionFile entity
    );

    // ---------- Post-processing ----------
    @AfterMapping
    default void linkFiles(@MappingTarget Description description) {
        if (description.getAttachments() != null) {
            description.getAttachments().forEach(f -> f.setDescription(description));
        }
    }
}