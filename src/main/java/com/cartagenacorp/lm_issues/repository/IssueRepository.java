package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.Issue;
import com.cartagenacorp.lm_issues.enums.IssueEnum.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    List<Issue> findByStatus(Status status);
}