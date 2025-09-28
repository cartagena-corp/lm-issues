package com.cartagenacorp.lm_issues.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "description")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Description {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "text", nullable = false, length = 5000)
    private String text;

    @ManyToOne()
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @OneToMany(mappedBy = "description", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DescriptionFile> attachments = new ArrayList<>();
}
