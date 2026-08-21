package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entities.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entities.PerformanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, Long>, JpaSpecificationExecutor<PerformanceCycle> {
    Optional<PerformanceCycle> findFirstByStatusOrderByStartDateDesc(CycleStatus status);
    boolean existsByNameIgnoreCase(String name);
}