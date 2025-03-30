package com.cartagenacorp.lm_issues.entity;

import com.cartagenacorp.lm_issues.enums.IssueEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "issue")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Description> descriptions = new ArrayList<>();

    @Column(name = "estimatedTime", nullable = false)
    private Integer estimatedTime;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private IssueEnum.Priority priority;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IssueEnum.Status status;

    @Column(name = "createdAt", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "reporterId", nullable = false)
    private UUID  reporterId;

    @Column(name = "assignedId")
    private UUID assignedId;
}
