package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PolicyAssignment;
import com.example.hrmspolicies2.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PolicyAssignmentRepository
        extends JpaRepository<PolicyAssignment, Long> {

    boolean existsByEmployee_IdAndPolicyVersion_Id(
            Long employeeId,
            Long policyVersionId
    );

    Optional<PolicyAssignment>
    findByEmployee_IdAndPolicyVersion_Id(
            Long employeeId,
            Long policyVersionId
    );

    List<PolicyAssignment>
    findByEmployee_IdOrderByDeadlineAsc(
            Long employeeId
    );

    List<PolicyAssignment>
    findByEmployee_IdAndPolicyVersion_CurrentVersionTrueOrderByDeadlineAsc(
            Long employeeId
    );

    List<PolicyAssignment>
    findByPolicyVersion_Id(
            Long policyVersionId
    );

    List<PolicyAssignment>
    findByStatusAndDeadlineBefore(
            AssignmentStatus status,
            LocalDate date
    );

    List<PolicyAssignment>
    findByStatusInAndDeadlineBefore(
            Collection<AssignmentStatus> statuses,
            LocalDate date
    );

    List<PolicyAssignment>
    findByPolicy_IdAndStatusIn(
            Long policyId,
            Collection<AssignmentStatus> statuses
    );

    long countByPolicyVersion_Id(
            Long policyVersionId
    );

    long countByPolicyVersion_IdAndStatus(
            Long policyVersionId,
            AssignmentStatus status
    );
}