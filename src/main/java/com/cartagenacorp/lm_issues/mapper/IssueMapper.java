package com.cartagenacorp.lm_issues.mapper;

import com.cartagenacorp.lm_issues.dto.IssueDTO;
import com.cartagenacorp.lm_issues.entity.Issue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DescriptionMapper.class})
public interface IssueMapper {
    @Mapping(target = "descriptions", source = "descriptionsDTO") //, qualifiedByName = "mapDescriptions"
    Issue issueDTOToIssue(IssueDTO issueDTO);

    @Mapping(target = "descriptionsDTO", source = "descriptions")
    IssueDTO issueToIssueDTO(Issue issue);

    List<IssueDTO> issuesToIssueDTOs(List<Issue> issues);

    List<Issue> issueDTOsToIssues(List<IssueDTO> issueDTOs);

}
