package com.cartagenacorp.lm_issues.repository.specifications;

import com.cartagenacorp.lm_issues.entity.Description;
import com.cartagenacorp.lm_issues.entity.Issue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class IssueSpecifications {
    public static Specification<Issue> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);

            String searchTerm = "%" + keyword.toLowerCase() + "%";

            Join<Issue, Description> descriptionsJoin = root.join("descriptions", JoinType.LEFT);

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(descriptionsJoin.get("text")), searchTerm)
            );
        };
    }

    public static Specification<Issue> hasProject(UUID projectId) {
        return (root, query, criteriaBuilder) -> {
            if (projectId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("projectId"), projectId);
        };
    }

    public static Specification<Issue> hasSprint(UUID sprintId) {
        return (root, query, criteriaBuilder) -> {
            if (sprintId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("sprintId"), sprintId);
        };
    }

    public static Specification<Issue> hasStatus(Long status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Issue> hasPriority(Long priority) {
        return (root, query, criteriaBuilder) -> {
            if (priority == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("priority"), priority);
        };
    }

    public static Specification<Issue> hasType(Long type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    public static Specification<Issue> hasAssigned(UUID assignedId) {
        return (root, query, criteriaBuilder) -> {
            if (assignedId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("assignedId"), assignedId);
        };
    }
}
