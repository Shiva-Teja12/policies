package com.practice.springbootdemo.advance_performance_module.entities;

import com.practice.springbootdemo.advance_performance_module.entities.GoalScope;
import com.practice.springbootdemo.advance_performance_module.entities.GoalStatus;
import com.practice.springbootdemo.advance_performance_module.entities.GoalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals",
        indexes = {
                @Index(name = "idx_goal_employee_cycle", columnList = "employee_id,cycle_id"),
                @Index(name = "idx_goal_manager", columnList = "manager_id"),
                @Index(name = "idx_goal_status", columnList = "status"),
                @Index(name = "idx_goal_due_date", columnList = "due_date")
        })
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_id", nullable = false)
    private Long cycleId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_scope", nullable = false, length = 30)
    @Builder.Default
    private GoalScope goalScope = GoalScope.INDIVIDUAL;

    @Column(name = "parent_goal_id")
    private Long parentGoalId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String target;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private GoalStatus status = GoalStatus.PENDING_ACCEPTANCE;

    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "employee_comment", length = 2000)
    private String employeeComment;

    @Column(name = "manager_comment", length = 2000)
    private String managerComment;

    @Builder.Default
    @Column(name = "modification_requested", nullable = false)
    private boolean modificationRequested = false;

    @Builder.Default
    @Column(name = "employee_accepted", nullable = false)
    private boolean employeeAccepted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (progress == null) progress = 0;
        if (status == null) status = GoalStatus.PENDING_ACCEPTANCE;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Goal(Long id, Long cycleId, Long employeeId, Long managerId, GoalType goalType, GoalScope goalScope, Long parentGoalId, String title, String description, String target, BigDecimal weight, LocalDate dueDate, GoalStatus status, Integer progress, String employeeComment, String managerComment, boolean modificationRequested, boolean employeeAccepted, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime completedAt) {
        this.id = id;
        this.cycleId = cycleId;
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.goalType = goalType;
        this.goalScope = goalScope;
        this.parentGoalId = parentGoalId;
        this.title = title;
        this.description = description;
        this.target = target;
        this.weight = weight;
        this.dueDate = dueDate;
        this.status = status;
        this.progress = progress;
        this.employeeComment = employeeComment;
        this.managerComment = managerComment;
        this.modificationRequested = modificationRequested;
        this.employeeAccepted = employeeAccepted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    public Goal() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCycleId() {
        return cycleId;
    }

    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
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

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public GoalScope getGoalScope() {
        return goalScope;
    }

    public void setGoalScope(GoalScope goalScope) {
        this.goalScope = goalScope;
    }

    public Long getParentGoalId() {
        return parentGoalId;
    }

    public void setParentGoalId(Long parentGoalId) {
        this.parentGoalId = parentGoalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getEmployeeComment() {
        return employeeComment;
    }

    public void setEmployeeComment(String employeeComment) {
        this.employeeComment = employeeComment;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public boolean isModificationRequested() {
        return modificationRequested;
    }

    public void setModificationRequested(boolean modificationRequested) {
        this.modificationRequested = modificationRequested;
    }

    public boolean isEmployeeAccepted() {
        return employeeAccepted;
    }

    public void setEmployeeAccepted(boolean employeeAccepted) {
        this.employeeAccepted = employeeAccepted;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}