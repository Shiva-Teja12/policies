package com.example.hrmspolicies2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "policy_acknowledgements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_ack_employee_version",
                        columnNames = {
                                "employee_id",
                                "policy_version_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_policy_ack_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_policy_ack_version",
                        columnList = "policy_version_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyAcknowledgement {

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

    @Column(
            name = "acknowledged_at",
            nullable = false
    )
    private LocalDateTime acknowledgedAt;

    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            length = 1000
    )
    private String userAgent;

    @PrePersist
    protected void beforeInsert() {
        if (acknowledgedAt == null) {
            acknowledgedAt = LocalDateTime.now();
        }
    }
}