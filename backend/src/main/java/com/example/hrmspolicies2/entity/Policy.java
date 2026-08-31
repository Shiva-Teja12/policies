package com.example.hrmspolicies2.entity;

import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_policies_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_policies_category",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_policies_mandatory",
                        columnList = "mandatory"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 200
    )
    private String name;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String code;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "category_id",
            nullable = false
    )
    private PolicyCategory category;

    /*
     * PostgreSQL uses TEXT instead of MySQL LONGTEXT.
     * Do not add @Lob here.
     */
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private Applicability applicability;

    @Column(
            name = "applicable_departments",
            length = 2000
    )
    private String applicableDepartments;

    @Column(
            name = "applicable_grades",
            length = 2000
    )
    private String applicableGrades;

    @Builder.Default
    @Column(nullable = false)
    private Boolean mandatory = false;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PolicyStatus status;

    @Builder.Default
    @Column(
            name = "acknowledgement_period_days",
            nullable = false
    )
    private Integer acknowledgementPeriodDays = 7;

    @Builder.Default
    @Column(
            name = "onboarding_period_days",
            nullable = false
    )
    private Integer onboardingPeriodDays = 7;

    @Builder.Default
    @Column(
            name = "published_once",
            nullable = false
    )
    private Boolean publishedOnce = false;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(
            name = "retirement_reason",
            length = 1000
    )
    private String retirementReason;

    @Column(name = "retirement_effective_date")
    private LocalDate retirementEffectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = PolicyStatus.DRAFT;
        }

        if (mandatory == null) {
            mandatory = false;
        }

        if (acknowledgementPeriodDays == null) {
            acknowledgementPeriodDays = 7;
        }

        if (onboardingPeriodDays == null) {
            onboardingPeriodDays = 7;
        }

        if (publishedOnce == null) {
            publishedOnce = false;
        }
    }

    @PreUpdate
    protected void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}