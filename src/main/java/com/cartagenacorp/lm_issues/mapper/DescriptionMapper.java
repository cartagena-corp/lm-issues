package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.DescriptionDtoRequest;
import com.cartagenacorp.lm_issues.dto.DescriptionDtoResponse;
import com.cartagenacorp.lm_issues.entity.Description;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DescriptionMapper {
    Description toEntity(DescriptionDtoRequest descriptionDtoRequest);

    DescriptionDtoResponse toDto(Description description);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Description partialUpdate(DescriptionDtoRequest descriptionDtoRequest, @MappingTarget Description description);
}