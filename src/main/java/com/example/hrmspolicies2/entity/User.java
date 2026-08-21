package com.example.hrmspolicies2.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Application user (HR admin or regular staff). Also the "owner" side
 * of a One-to-Many relationship with {@link Policy}: one User can
 * create many policies.
 *
 * {@code @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})}
 * is required because {@link Policy#getCreatedBy()} is a LAZY
 * association: Jackson would otherwise try to serialize Hibernate's
 * internal proxy fields (hibernateLazyInitializer/handler) whenever a
 * Policy with an uninitialized creator is returned from the API,
 * throwing InvalidDefinitionException / HttpMessageConversionException.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private String role;

    /**
     * One-to-Many: inverse side of {@link Policy#getCreatedBy()}.
     * mappedBy means the foreign key (created_by) lives on the Policy
     * table, not here. LAZY + no cascade so fetching a User never
     * pulls every policy they ever created, and deleting a User does
     * not delete their policies.
     */
    @Builder.Default
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Policy> createdPolicies = new ArrayList<>();
}
