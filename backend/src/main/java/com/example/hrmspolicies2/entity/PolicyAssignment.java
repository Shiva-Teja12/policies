package com.example.hrmspolicies2.entity;

import com.example.hrmspolicies2.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "policy_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_assignment_employee_version",
                        columnNames = {
                                "employee_id",
                                "policy_version_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_policy_assignments_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_policy_assignments_deadline",
                        columnList = "deadline"
                ),
                @Index(
                        name = "idx_policy_assignments_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "employee_id",
            nullable = false
    )
    private User employee;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "policy_id",
            nullable = false
    )
    private Policy policy;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "policy_version_id",
            nullable = false
    )
    private PolicyVersion policyVersion;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStatus status;

    @Column(
            name = "assigned_at",
            nullable = false
    )
    private LocalDateTime assignedAt;

    @Builder.Default
    @Column(
            name = "onboarding_assignment",
            nullable = false
    )
    private Boolean onboardingAssignment = false;

    @PrePersist
    protected void beforeInsert() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = AssignmentStatus.PENDING;
        }

        if (onboardingAssignment == null) {
            onboardingAssignment = false;
        }
    }
}