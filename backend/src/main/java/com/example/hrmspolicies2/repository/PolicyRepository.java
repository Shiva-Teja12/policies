package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.enums.PolicyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository
        extends JpaRepository<Policy, Long>,
        JpaSpecificationExecutor<Policy> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(
            String code,
            Long id
    );

    List<Policy> findByStatus(
            PolicyStatus status
    );

    List<Policy> findByStatusAndMandatoryTrue(
            PolicyStatus status
    );

    @EntityGraph(
            attributePaths = {
                    "category",
                    "createdBy"
            }
    )
    Optional<Policy> findDetailedById(
            Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Policy p
            where p.id = :id
            """)
    Optional<Policy> findByIdForUpdate(
            @Param("id") Long id
    );
}