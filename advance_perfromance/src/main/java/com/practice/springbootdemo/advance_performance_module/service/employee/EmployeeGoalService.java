package com.practice.springbootdemo.advance_performance_module.service.employee;


import com.practice.springbootdemo.advance_performance_module.entities.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entities.Goal;
import com.practice.springbootdemo.advance_performance_module.entities.PerformanceCycle;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidGoalStatusException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmployeeGoalService {
    private final GoalRepository goalRepository;
    private final PerformanceCycleRepository cycleRepository;
    public EmployeeGoalService(GoalRepository goalRepository, PerformanceCycleRepository cycleRepository) {
        this.goalRepository = goalRepository;
        this.cycleRepository = cycleRepository;
    }
    @Transactional(readOnly = true)
    public List<EmployeeGoalResponse> getMyGoals(Long employeeId) {
        PerformanceCycle activeCycle = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE)
                .orElse(null);
        List<Goal> goals = (activeCycle != null)
                ? goalRepository.findByEmployeeIdAndCycleId(employeeId, activeCycle.getId())
                : goalRepository.findByEmployeeId(employeeId);
        return goals.stream().map(this::mapToResponse).toList();
    }
    @Transactional(readOnly = true)
    public EmployeeGoalResponse getGoalById(Long goalId, Long employeeId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getEmployeeId().equals(employeeId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot access another employee's goal");
        }
        return mapToResponse(goal);
    }
    @Transactional
    public EmployeeGoalResponse acceptGoal(Long goalId, Long employeeId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getEmployeeId().equals(employeeId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot accept another employee's goal");
        }
        if (goal.getStatus() != GoalStatus.PENDING_ACCEPTANCE) {
            throw new InvalidGoalStatusException("Goal is not in PENDING_ACCEPTANCE status (Current: " + goal.getStatus() + ")");
        }
        goal.setStatus(GoalStatus.ACCEPTED);
        goal.setEmployeeAccepted(true);
        Goal saved = goalRepository.save(goal);
        log.info("Employee {} accepted Goal ID {}", employeeId, goalId);
        return mapToResponse(saved);
    }
    @Transactional
    public EmployeeGoalResponse updateProgress(Long goalId, Long employeeId, GoalProgressUpdateRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getEmployeeId().equals(employeeId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot update progress for another employee's goal");
        }
        if (goal.getStatus() == GoalStatus.PENDING_ACCEPTANCE || goal.getStatus() == GoalStatus.REJECTED) {
            throw new InvalidGoalStatusException("Cannot update progress on a goal with status: " + goal.getStatus());
        }
        goal.setProgress(request.progress());
        if (request.comment() != null && !request.comment().isBlank()) {
            goal.setEmployeeComment(request.comment());
        }
        if (request.progress() == 100) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
            log.info("Goal ID {} marked COMPLETED by Employee {}", goalId, employeeId);
        } else if (request.progress() > 0 && goal.getStatus() == GoalStatus.ACCEPTED) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }
        Goal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }
    private EmployeeGoalResponse mapToResponse(Goal g) {
        return new EmployeeGoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getGoalType(),
                g.getGoalScope(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isEmployeeAccepted(),
                g.isModificationRequested(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        );
    }