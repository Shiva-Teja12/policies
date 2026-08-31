package com.example.hrmspolicies2.entity;

import com.example.hrmspolicies2.enums.ApprovalDecision;
import com.example.hrmspolicies2.enums.ApprovalStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "policy_approvals",
        indexes = {
                @Index(
                        name = "idx_policy_approvals_policy",
                        columnList = "policy_id"
                ),
                @Index(
                        name = "idx_policy_approvals_approver",
                        columnList = "approver_id"
                ),
                @Index(
                        name = "idx_policy_approvals_queue",
                        columnList = "stage,decision"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "policy_id",
            nullable = false
    )
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Column(length = 2000)
    private String comments;

    @Column(
            name = "submitted_at",
            nullable = false
    )
    private LocalDateTime submittedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Builder.Default
    @Column(
            name = "sequence_number",
            nullable = false
    )
    private Integer sequenceNumber = 1;

    @PrePersist
    protected void beforeInsert() {
        if (decision == null) {
            decision = ApprovalDecision.PENDING;
        }

        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}