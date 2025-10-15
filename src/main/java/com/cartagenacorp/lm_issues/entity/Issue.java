package com.cartagenacorp.lm_issues.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Description> descriptions = new ArrayList<>();

    @Column(name = "estimatedTime")
    private Integer estimatedTime;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "priority")
    private Long priority;

    @Column(name = "status")
    private Long status;

    @Column(name = "type")
    private Long type;

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

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "real_date")
    private LocalDate realDate;

    @Column(name = "organization_id")
    private UUID organizationId;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonBackReference
    private Issue parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Issue> subtasks = new ArrayList<>();

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueRelation> relatedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueRelation> relatedTo = new ArrayList<>();

    public Issue(Issue other) {
        this.id = other.id;
        this.title = other.title;
        this.estimatedTime = other.estimatedTime;
        this.projectId = other.projectId;
        this.sprintId = other.sprintId;
        this.priority = other.priority;
        this.status = other.status;
        this.type = other.type;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.reporterId = other.reporterId;
        this.assignedId = other.assignedId;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.realDate = other.realDate;
        if(other.descriptions != null) {
            this.descriptions = other.descriptions.stream()
                    .map(Description::new)
                    .collect(Collectors.toList());
        }
    }
}
