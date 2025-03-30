package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.DescriptionDTO;
import com.cartagenacorp.lm_issues.entity.Description;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DescriptionMapper {

    @Mapping(target = "issueId", source = "issue.id")
    DescriptionDTO descriptionToDescriptionDTO(Description description);

    @Mapping(target = "issue.id", source = "issueId")
    Description descriptionDTOToDescription(DescriptionDTO descriptionDTO);
}