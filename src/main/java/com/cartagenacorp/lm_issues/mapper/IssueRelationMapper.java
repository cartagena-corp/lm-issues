package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.IssueRelationDto;
import com.cartagenacorp.lm_issues.entity.IssueRelation;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface IssueRelationMapper {
    IssueRelation toEntity(IssueRelationDto issueRelationDto);

    @Mapping(source = "target.id", target = "targetId")
    @Mapping(source = "target.title", target = "targetTitle")
    IssueRelationDto toDto(IssueRelation issueRelation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    IssueRelation partialUpdate(IssueRelationDto issueRelationDto, @MappingTarget IssueRelation issueRelation);
}