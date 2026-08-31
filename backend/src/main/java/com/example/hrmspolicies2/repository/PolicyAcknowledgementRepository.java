package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PolicyAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyAcknowledgementRepository
        extends JpaRepository<PolicyAcknowledgement, Long> {

    Optional<PolicyAcknowledgement>
    findByEmployee_IdAndPolicyVersion_Id(
            Long employeeId,
            Long policyVersionId
    );

    boolean existsByEmployee_IdAndPolicyVersion_Id(
            Long employeeId,
            Long policyVersionId
    );

    List<PolicyAcknowledgement>
    findByEmployee_IdOrderByAcknowledgedAtDesc(
            Long employeeId
    );

    long countByPolicyVersion_Id(
            Long policyVersionId
    );
}