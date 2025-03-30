package com.cartagenacorp.lm_issues.config;

import com.cartagenacorp.lm_issues.mapper.DescriptionMapper;
import com.cartagenacorp.lm_issues.mapper.IssueMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    public IssueMapper issueMapper() {
        return Mappers.getMapper(IssueMapper.class);
    }
    @Bean
    public DescriptionMapper descriptionMapper() {
        return Mappers.getMapper(DescriptionMapper.class);
    }
}
