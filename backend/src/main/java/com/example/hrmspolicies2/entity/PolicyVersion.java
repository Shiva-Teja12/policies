package com.example.hrmspolicies2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "policy_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_version_number",
                        columnNames = {
                                "policy_id",
                                "version_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_policy_versions_policy",
                        columnList = "policy_id"
                ),
                @Index(
                        name = "idx_policy_versions_current",
                        columnList = "policy_id,current_version"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyVersion {

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

    @Column(
            name = "version_number",
            nullable = false
    )
    private Integer versionNumber;

    /*
     * PostgreSQL uses TEXT instead of MySQL LONGTEXT.
     * Do not add @Lob here.
     */
    @Column(
            name = "content_snapshot",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String contentSnapshot;

    @Column(
            name = "effective_date",
            nullable = false
    )
    private LocalDate effectiveDate;

    @Column(
            name = "published_at",
            nullable = false
    )
    private LocalDateTime publishedAt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "published_by",
            nullable = false
    )
    private User publishedBy;

    @Column(
            name = "change_summary",
            length = 1000
    )
    private String changeSummary;

    @Builder.Default
    @Column(
            name = "current_version",
            nullable = false
    )
    private Boolean currentVersion = true;

    @Builder.Default
    @Column(
            name = "archived",
            nullable = false
    )
    private Boolean archived = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void beforeInsert() {
        createdAt = LocalDateTime.now();

        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }

        if (currentVersion == null) {
            currentVersion = true;
        }

        if (archived == null) {
            archived = false;
        }
    }
}