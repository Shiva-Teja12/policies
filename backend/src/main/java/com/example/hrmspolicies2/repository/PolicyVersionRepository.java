package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.hrmspolicies2.enums.PolicyStatus;
import java.util.List;
import java.util.Optional;

public interface PolicyVersionRepository
        extends JpaRepository<PolicyVersion, Long> {

    Optional<PolicyVersion>
    findTopByPolicy_IdOrderByVersionNumberDesc(
            Long policyId
    );

    Optional<PolicyVersion>
    findByPolicy_IdAndCurrentVersionTrue(
            Long policyId
    );

    Optional<PolicyVersion>
    findByPolicy_IdAndVersionNumber(
            Long policyId,
            Integer versionNumber
    );

    List<PolicyVersion>
    findByPolicy_IdOrderByVersionNumberDesc(
            Long policyId
    );

    List<PolicyVersion>
    findByCurrentVersionTrueAndPolicy_Status(
            PolicyStatus status
    );
}