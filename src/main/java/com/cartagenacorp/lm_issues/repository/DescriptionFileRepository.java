package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.DescriptionFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DescriptionFileRepository extends JpaRepository<DescriptionFile, UUID> {
}