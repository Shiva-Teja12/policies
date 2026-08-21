package com.example.hrmspolicies2.specification;

import com.example.hrmspolicies2.entity.Policy;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a single dynamic {@link Specification} for Policy search out of
 * whichever filter parameters the caller actually supplied. Any
 * parameter left null/blank is simply skipped, so GET /api/policies/search
 * works whether the client sends one filter or five at once.
 */
public final class PolicySpecification {

    private PolicySpecification() {
    }

    public static Specification<Policy> filterBy(
            String keyword,
            String category,
            String status,
            String applicability,
            Boolean mandatory
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Free-text search across name, code and content
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.toLowerCase() + "%";

                Predicate nameLike = cb.like(cb.lower(root.get("name")), likePattern);
                Predicate codeLike = cb.like(cb.lower(root.get("code")), likePattern);
                Predicate contentLike = cb.like(cb.lower(root.get("content")), likePattern);

                predicates.add(cb.or(nameLike, codeLike, contentLike));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
            }

            if (StringUtils.hasText(applicability)) {
                predicates.add(cb.equal(cb.lower(root.get("applicability")), applicability.toLowerCase()));
            }

            if (mandatory != null) {
                predicates.add(cb.equal(root.get("mandatory"), mandatory));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
