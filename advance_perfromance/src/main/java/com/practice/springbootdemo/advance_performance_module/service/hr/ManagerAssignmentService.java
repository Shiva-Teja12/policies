package com.practice.springbootdemo.advance_performance_module.service.hr;


import com.practice.springbootdemo.advance_performance_module.entities.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ManagerAssignmentService {
    private final ManagerAssignmentRepository repository;
    private final UserRepository userRepository;
    private final PerformanceCycleRepository cycleRepository;
    public ManagerAssignmentService(
            ManagerAssignmentRepository repository,
            UserRepository userRepository,
            PerformanceCycleRepository cycleRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.cycleRepository = cycleRepository;
    }
    @Transactional
    public AssignmentResponse assign(AssignManagerRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.managerId()));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("User ID " + request.managerId() + " does not have MANAGER role");
        }
        User employee = userRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.employeeId()));
        if (employee.getRole() != Role.EMPLOYEE) {
            throw new BadRequestException("User ID " + request.employeeId() + " does not have EMPLOYEE role");
        }
        if (request.performanceCycleId() != null) {
            PerformanceCycle cycle = cycleRepository.findById(request.performanceCycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + request.performanceCycleId()));
            if (cycle.getStatus() == CycleStatus.CLOSED) {
                throw new BadRequestException("Cannot assign manager in a CLOSED performance cycle");
            }
        }
        if (repository.existsByEmployeeIdAndManagerIdAndActiveTrue(request.employeeId(), request.managerId())) {
            throw new DuplicateResourceException("Manager is already assigned to this employee");
        }
        repository.findByEmployeeIdAndActiveTrue(request.employeeId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    repository.save(existing);
                    log.info("Deactivated previous manager assignment ID: {}", existing.getId());
                });
        ManagerAssignment assignment = ManagerAssignment.builder()
                .managerId(manager.getId())
                .employeeId(employee.getId())
                .performanceCycleId(request.performanceCycleId())
                .active(true)
                .assignedDate(LocalDateTime.now())
                .build();
        ManagerAssignment saved = repository.save(assignment);
        log.info("Assigned Manager {} to Employee {} for Cycle {}", manager.getId(), employee.getId(), request.performanceCycleId());
        return mapToResponse(saved, manager, employee);
    }
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments() {
        return repository.findAll().stream()
                .map(a -> {
                    User manager = userRepository.findById(a.getManagerId()).orElse(null);
                    User employee = userRepository.findById(a.getEmployeeId()).orElse(null);
                    return mapToResponse(a, manager, employee);
                })
                .toList();
    }
    @Transactional(readOnly = true)
    public AssignmentResponse getById(Long id) {
        ManagerAssignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found with ID: " + id));
        User manager = userRepository.findById(assignment.getManagerId()).orElse(null);
        User employee = userRepository.findById(assignment.getEmployeeId()).orElse(null);
        return mapToResponse(assignment, manager, employee);
    }
    @Transactional
    public AssignmentResponse update(Long id, UpdateAssignmentRequest request) {
        ManagerAssignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found with ID: " + id));
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.managerId()));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("User ID " + request.managerId() + " does not have MANAGER role");
        }
        assignment.setManagerId(manager.getId());
        ManagerAssignment saved = repository.save(assignment);
        User employee = userRepository.findById(assignment.getEmployeeId()).orElse(null);
        return mapToResponse(saved, manager, employee);
    }
    @Transactional
    public void delete(Long id) {
        ManagerAssignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found with ID: " + id));
        assignment.setActive(false);
        repository.save(assignment);
        log.info("Manager assignment ID {} deactivated", id);
    }
    private AssignmentResponse mapToResponse(ManagerAssignment a, User manager, User employee) {
        return new AssignmentResponse(
                a.getId(),
                a.getManagerId(),
                manager != null ? manager.getName() : "Unknown",
                manager != null ? manager.getEmail() : "Unknown",
                a.getEmployeeId(),
                employee != null ? employee.getName() : "Unknown",
                employee != null ? employee.getEmail() : "Unknown",
                a.getPerformanceCycleId(),
                a.isActive(),
                a.getAssignedDate()
        );
    }
