package com.example.hrmspolicies2.specification;

import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PolicySpecification {

    private PolicySpecification() {
    }

    public static Specification<Policy> filterBy(
            String search,
            Long categoryId,
            PolicyStatus status,
            Applicability applicability,
            Boolean mandatory,
            boolean managementView
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (query != null) {
                query.distinct(true);
            }

            if (StringUtils.hasText(search)) {
                String keyword =
                        "%"
                                + search.trim()
                                .toLowerCase(Locale.ROOT)
                                + "%";

                predicates.add(
                        builder.or(
                                builder.like(
                                        builder.lower(
                                                root.get("name")
                                        ),
                                        keyword
                                ),
                                builder.like(
                                        builder.lower(
                                                root.get("code")
                                        ),
                                        keyword
                                ),
                                builder.like(
                                        builder.lower(
                                                root.get("content")
                                        ),
                                        keyword
                                )
                        )
                );
            }

            if (categoryId != null) {
                predicates.add(
                        builder.equal(
                                root.get("category")
                                        .get("id"),
                                categoryId
                        )
                );
            }

            /*
             * Management roles may filter by any policy status.
             *
             * Employee and Manager accounts are always restricted
             * to PUBLISHED policies. Supplying status=DRAFT,
             * APPROVED, LEGAL_REVIEW, etc. cannot bypass this rule.
             */
            if (managementView) {
                if (status != null) {
                    predicates.add(
                            builder.equal(
                                    root.get("status"),
                                    status
                            )
                    );
                }
            } else {
                predicates.add(
                        builder.equal(
                                root.get("status"),
                                PolicyStatus.PUBLISHED
                        )
                );
            }

            if (applicability != null) {
                predicates.add(
                        builder.equal(
                                root.get("applicability"),
                                applicability
                        )
                );
            }

            if (mandatory != null) {
                predicates.add(
                        builder.equal(
                                root.get("mandatory"),
                                mandatory
                        )
                );
            }

            return builder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}