package com.practice.springbootdemo.advance_performance_module.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_assignments",
        indexes = {
                @Index(name = "idx_assignment_employee_active", columnList = "employee_id,active"),
                @Index(name = "idx_assignment_manager_active", columnList = "manager_id,active"),
                @Index(name = "idx_assignment_cycle", columnList = "performance_cycle_id")
        })
@Builder
public class ManagerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "performance_cycle_id")
    private Long performanceCycleId;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (assignedDate == null) assignedDate = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ManagerAssignment(Long id, Long employeeId, Long managerId, Long performanceCycleId, boolean active, LocalDateTime assignedDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.performanceCycleId = performanceCycleId;
        this.active = active;
        this.assignedDate = assignedDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ManagerAssignment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Long getPerformanceCycleId() {
        return performanceCycleId;
    }

    public void setPerformanceCycleId(Long performanceCycleId) {
        this.performanceCycleId = performanceCycleId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}