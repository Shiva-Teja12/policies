package com.example.hrmspolicies2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Core entity of the HRMS Policies module.
 *
 * Relationship: many Policy rows can be authored by one User
 * (Many-to-One from Policy -> User, mirrored as One-to-Many on
 * {@link User#getCreatedPolicies()}). The association is LAZY so that
 * loading a Policy never triggers an extra query for the creator unless
 * it is explicitly accessed.
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String applicability;

    @Column(nullable = false)
    private Boolean mandatory;

    @Column(nullable = false)
    private String status;

    /**
     * Many-to-One: several policies can be created by the same user.
     * fetch = LAZY so the owning User is only loaded when accessed.
     * No cascade here on purpose - deleting a Policy must never
     * cascade into deleting the User who authored it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    @ToString.Exclude
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
