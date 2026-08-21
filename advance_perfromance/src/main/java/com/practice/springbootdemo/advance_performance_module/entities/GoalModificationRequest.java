package com.practice.springbootdemo.advance_performance_module.entities;

import com.practice.springbootdemo.advance_performance_module.entities.ModificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "goal_modification_requests",
        indexes = {
                @Index(name = "idx_mod_goal", columnList = "goal_id"),
                @Index(name = "idx_mod_employee", columnList = "employee_id"),
                @Index(name = "idx_mod_manager", columnList = "manager_id"),
                @Index(name = "idx_mod_status", columnList = "status")
        })
@Builder
public class GoalModificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "requested_changes", length = 2000)
    private String requestedChanges;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ModificationStatus status = ModificationStatus.PENDING;

    @Column(name = "manager_comment", length = 2000)
    private String managerComment;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }

    public GoalModificationRequest(Long id, Long goalId, Long employeeId, Long managerId, String requestedChanges, String comment, ModificationStatus status, String managerComment, LocalDateTime requestedAt, LocalDateTime reviewedAt, Long reviewedBy) {
        this.id = id;
        this.goalId = goalId;
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.requestedChanges = requestedChanges;
        this.comment = comment;
        this.status = status;
        this.managerComment = managerComment;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
    }

    public GoalModificationRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
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

    public String getRequestedChanges() {
        return requestedChanges;
    }

    public void setRequestedChanges(String requestedChanges) {
        this.requestedChanges = requestedChanges;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ModificationStatus getStatus() {
        return status;
    }

    public void setStatus(ModificationStatus status) {
        this.status = status;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}