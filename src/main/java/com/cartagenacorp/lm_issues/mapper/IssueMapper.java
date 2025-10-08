package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.dto.IssueDtoRequest;
import com.cartagenacorp.lm_issues.dto.IssueDtoResponse;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {DescriptionMapper.class})
public interface IssueMapper {
    Issue toEntity(IssueDtoRequest issueDtoRequest);

    @Mapping(target = "reporterId", ignore = true)
    @Mapping(target = "assignedId", ignore = true)
    IssueDtoResponse toDto(Issue issue);

    @AfterMapping
    default void linkDescriptions(@MappingTarget Issue issue) {
        if (issue.getDescriptions() != null) {
            issue.getDescriptions().forEach(description -> description.setIssue(issue));
        }
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Issue partialUpdate(IssueDtoRequest issueDtoRequest, @MappingTarget Issue issue);


    @Mapping(target = "descriptions", source = "descriptionsDTO")
    Issue toEntityImport(IssueDTO issueDTO);

    @Mapping(target = "descriptionsDTO", source = "descriptions")
    IssueDTO toDtoImport(Issue issue);
}
