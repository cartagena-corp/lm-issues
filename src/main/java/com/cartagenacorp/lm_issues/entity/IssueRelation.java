package com.cartagenacorp.lm_issues.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issue_relation")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_issue_id", nullable = false)
    private Issue source;

    @ManyToOne
    @JoinColumn(name = "target_issue_id", nullable = false)
    private Issue target;
}
