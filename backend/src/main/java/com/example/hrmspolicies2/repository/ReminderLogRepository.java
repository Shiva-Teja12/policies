package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.ReminderLog;
import com.example.hrmspolicies2.enums.ReminderStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderLogRepository
        extends JpaRepository<ReminderLog, Long> {

    boolean existsByAssignment_IdAndReminderStage(
            Long assignmentId,
            ReminderStage reminderStage
    );

    Page<ReminderLog>
    findByEmployee_Id(
            Long employeeId,
            Pageable pageable
    );

    Page<ReminderLog>
    findByPolicy_Id(
            Long policyId,
            Pageable pageable
    );
}