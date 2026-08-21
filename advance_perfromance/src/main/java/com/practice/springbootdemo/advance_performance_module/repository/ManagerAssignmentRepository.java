package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entities.ManagerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerAssignmentRepository extends JpaRepository<ManagerAssignment, Long>, JpaSpecificationExecutor<ManagerAssignment> {
    Optional<ManagerAssignment> findByEmployeeIdAndActiveTrue(Long employeeId);
    List<ManagerAssignment> findByManagerIdAndActiveTrue(Long managerId);
    boolean existsByEmployeeIdAndActiveTrue(Long employeeId);
    boolean existsByEmployeeIdAndManagerIdAndActiveTrue(Long employeeId, Long managerId);
    List<ManagerAssignment> findByPerformanceCycleIdAndActiveTrue(Long cycleId);
}
