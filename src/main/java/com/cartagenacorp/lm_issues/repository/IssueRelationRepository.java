package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.IssueRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueRelationRepository extends JpaRepository<IssueRelation, UUID> {
    List<IssueRelation> findBySource_Id(UUID sourceId);

    List<IssueRelation> findByTarget_Id(UUID targetId);
}