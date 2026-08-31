package com.example.hrmspolicies2.entity;

import com.example.hrmspolicies2.enums.NotificationStatus;
import com.example.hrmspolicies2.enums.ReminderStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reminder_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reminder_assignment_stage",
                        columnNames = {
                                "assignment_id",
                                "reminder_stage"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_reminder_logs_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_reminder_logs_status",
                        columnList = "delivery_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "assignment_id",
            nullable = false
    )
    private PolicyAssignment assignment;

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "reminder_stage",
            nullable = false,
            length = 40
    )
    private ReminderStage reminderStage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "delivery_status",
            nullable = false,
            length = 30
    )
    private NotificationStatus deliveryStatus;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "cc_email")
    private String ccEmail;

    @Column(
            name = "sent_at",
            nullable = false
    )
    private LocalDateTime sentAt;

    @Column(
            name = "failure_reason",
            length = 2000
    )
    private String failureReason;

    @PrePersist
    protected void beforeInsert() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }

        if (deliveryStatus == null) {
            deliveryStatus =
                    NotificationStatus.PENDING;
        }
    }
}