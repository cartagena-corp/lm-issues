package com.cartagenacorp.lm_issues.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Issue issue;

    @OneToMany(mappedBy = "description", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DescriptionFile> attachments = new ArrayList<>();

    public Description(Description other) {
        this.id = other.id;
        this.title = other.title;
        this.text = other.text;
        if(other.attachments != null) {
            this.attachments = new ArrayList<>(other.attachments);
        }
    }
}
