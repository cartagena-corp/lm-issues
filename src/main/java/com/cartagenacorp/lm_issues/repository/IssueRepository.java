package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
}
