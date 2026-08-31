package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PolicyApproval;
import com.example.hrmspolicies2.enums.ApprovalDecision;
import com.example.hrmspolicies2.enums.ApprovalStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyApprovalRepository
        extends JpaRepository<PolicyApproval, Long> {

    List<PolicyApproval>
    findByPolicy_IdOrderBySubmittedAtAsc(
            Long policyId
    );

    Optional<PolicyApproval>
    findFirstByPolicy_IdAndDecisionOrderBySubmittedAtDesc(
            Long policyId,
            ApprovalDecision decision
    );

    Optional<PolicyApproval>
    findFirstByPolicy_IdAndStageAndDecisionOrderBySubmittedAtDesc(
            Long policyId,
            ApprovalStage stage,
            ApprovalDecision decision
    );

    boolean existsByPolicy_IdAndDecision(
            Long policyId,
            ApprovalDecision decision
    );

    Page<PolicyApproval>
    findByStageAndDecision(
            ApprovalStage stage,
            ApprovalDecision decision,
            Pageable pageable
    );
}