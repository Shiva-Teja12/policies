package com.practice.springbootdemo.advance_performance_module.service.manager;

import com.practice.springbootdemo.advance_performance_module.entities.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.BusinessAuthorizationException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.GoalRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class ManagerGoalService {
    private final GoalRepository goalRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final UserRepository userRepository;
    public ManagerGoalService(
            GoalRepository goalRepository,
            ManagerAssignmentRepository assignmentRepository,
            PerformanceCycleRepository cycleRepository,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.assignmentRepository = assignmentRepository;
        this.cycleRepository = cycleRepository;
        this.userRepository = userRepository;
    }
    @Transactional
    public GoalResponse create(CreateGoalRequest request, Long managerId) {
        boolean assigned = assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(request.employeeId(), managerId);
        if (!assigned) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not assigned as this employee's manager");
        }
        PerformanceCycle cycle = cycleRepository.findById(request.cycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + request.cycleId()));
        if (cycle.getStatus() != CycleStatus.ACTIVE) {
            throw new InvalidPerformanceCycleException("Goals can only be created in an ACTIVE performance cycle (Current: " + cycle.getStatus() + ")");
        }
        BigDecimal currentTotal = goalRepository.totalWeight(request.employeeId(), request.cycleId());
        BigDecimal newTotal = currentTotal.add(request.weight());
        if (newTotal.compareTo(BigDecimal.valueOf(100.00)) > 0) {
            throw new BadRequestException(String.format("Total weight cannot exceed 100.00%%. Current: %.2f%%, Requested: %.2f%%", currentTotal, request.weight()));
        }
        Goal goal = Goal.builder()
                .cycleId(request.cycleId())
                .employeeId(request.employeeId())
                .managerId(managerId)
                .goalType(request.goalType())
                .goalScope(request.goalScope() != null ? request.goalScope() : GoalScope.INDIVIDUAL)
                .parentGoalId(request.parentGoalId())
                .title(request.title().trim())
                .description(request.description())
                .target(request.target())
                .weight(request.weight())
                .dueDate(request.dueDate())
                .status(GoalStatus.PENDING_ACCEPTANCE)
                .progress(0)
                .employeeAccepted(false)
                .modificationRequested(false)
                .build();
        Goal saved = goalRepository.save(goal);
        log.info("Manager {} created Goal ID {} for Employee {}", managerId, saved.getId(), request.employeeId());
        return mapToResponse(saved);
    }
    @Transactional
    public GoalResponse update(Long goalId, UpdateGoalRequest request, Long managerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getManagerId().equals(managerId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You can only update goals created by you");
        }
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a COMPLETED goal");
        }
        BigDecimal currentTotalOther = goalRepository.totalWeightExcluding(goal.getEmployeeId(), goal.getCycleId(), goal.getId());
        BigDecimal newTotal = currentTotalOther.add(request.weight());
        if (newTotal.compareTo(BigDecimal.valueOf(100.00)) > 0) {
            throw new BadRequestException(String.format("Total weight exceeds 100.00%%. Current other goals: %.2f%%, Requested: %.2f%%", currentTotalOther, request.weight()));
        }
        goal.setTitle(request.title().trim());
        goal.setDescription(request.description());
        goal.setTarget(request.target());
        goal.setWeight(request.weight());
        goal.setDueDate(request.dueDate());
        Goal saved = goalRepository.save(goal);
        log.info("Manager {} updated Goal ID {}", managerId, goalId);
        return mapToResponse(saved);
    }
    @Transactional
    public void delete(Long goalId, Long managerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getManagerId().equals(managerId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You cannot delete another manager's goal");
        }
        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getProgress() > 0) {
            throw new BadRequestException("Cannot delete a goal with recorded progress or COMPLETED status");
        }
        goalRepository.delete(goal);
        log.info("Manager {} deleted Goal ID {}", managerId, goalId);
    }
    @Transactional(readOnly = true)
    public GoalResponse getGoal(Long goalId, Long managerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with ID: " + goalId));
        if (!goal.getManagerId().equals(managerId)) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You do not have access to this goal");
        }
        return mapToResponse(goal);
    }
    @Transactional(readOnly = true)
    public List<GoalResponse> getAllManagerGoals(Long managerId) {
        return goalRepository.findByManagerId(managerId).stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<GoalResponse> getEmployeeGoals(Long employeeId, Long cycleId, Long managerId) {
        boolean assigned = assignmentRepository.existsByEmployeeIdAndManagerIdAndActiveTrue(employeeId, managerId);
        if (!assigned) {
            throw new BusinessAuthorizationException("Business Authorization Denied: You are not assigned to this employee");
        }
        return goalRepository.findByEmployeeIdAndCycleId(employeeId, cycleId).stream()
                .map(this::mapToResponse)
                .toList();
    }
    private GoalResponse mapToResponse(Goal g) {
        String employeeName = userRepository.findById(g.getEmployeeId()).map(User::getName).orElse("Unknown");
        String managerName = userRepository.findById(g.getManagerId()).map(User::getName).orElse("Unknown");
        return new GoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getEmployeeId(),
                employeeName,
                g.getManagerId(),
                managerName,
                g.getGoalType(),
                g.getGoalScope(),
                g.getParentGoalId(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isModificationRequested(),
                g.isEmployeeAccepted(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        );
    }