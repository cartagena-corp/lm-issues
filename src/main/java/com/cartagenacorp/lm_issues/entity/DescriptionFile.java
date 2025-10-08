package com.cartagenacorp.lm_issues.entity;

import com.cartagenacorp.lm_issues.util.DescriptionFileListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@EntityListeners(DescriptionFileListener.class)
@Table(name = "description_file")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @ManyToOne()
    @JoinColumn(name = "description_id", nullable = false)
    private Description description;
}

