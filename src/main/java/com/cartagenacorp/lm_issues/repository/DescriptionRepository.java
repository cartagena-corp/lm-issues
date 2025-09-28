package com.cartagenacorp.lm_issues.repository;

import com.cartagenacorp.lm_issues.entity.Description;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DescriptionRepository extends JpaRepository<Description, UUID> {
}