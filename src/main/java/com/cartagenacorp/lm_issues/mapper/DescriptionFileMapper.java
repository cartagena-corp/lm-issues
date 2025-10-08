package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.DescriptionFileDtoRequest;
import com.cartagenacorp.lm_issues.dto.DescriptionFileDtoResponse;
import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DescriptionFileMapper {
    DescriptionFile toEntity(DescriptionFileDtoRequest descriptionFileDto);

    DescriptionFileDtoResponse toDto(DescriptionFile descriptionFile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DescriptionFile partialUpdate(DescriptionFileDtoRequest descriptionFileDto, @MappingTarget DescriptionFile descriptionFile);
}