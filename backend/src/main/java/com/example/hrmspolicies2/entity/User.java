package com.example.hrmspolicies2.entity;

import com.example.hrmspolicies2.enums.AccountStatus;
import com.example.hrmspolicies2.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_users_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_users_role",
                        columnList = "role"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            length = 120
    )
    private String name;

    @Column(
            nullable = false,
            unique = true,
            length = 190
    )
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 30
    )
    private AccountStatus accountStatus =
            AccountStatus.ACTIVE;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String grade;

    @Column(
            name = "manager_email",
            length = 255
    )
    private String managerEmail;

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

    @Builder.Default
    @OneToMany(
            mappedBy = "createdBy",
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Policy> createdPolicies =
            new ArrayList<>();

    @PrePersist
    protected void beforeInsert() {
        LocalDateTime now =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        createdAt = now;
        updatedAt = now;

        if (accountStatus == null) {
            accountStatus =
                    AccountStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void beforeUpdate() {
        updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );
    }
}