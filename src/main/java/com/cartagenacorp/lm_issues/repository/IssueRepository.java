package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    Page<Issue> findByStatus(String status, Pageable pageable);

    Page<Issue> findByProjectId(UUID projectId, Pageable pageable);

    boolean existsById(UUID id);

    List<Issue> findByParentId(UUID parentId);
}