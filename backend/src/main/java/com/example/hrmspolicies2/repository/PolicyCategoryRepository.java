package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyCategoryRepository
        extends JpaRepository<PolicyCategory, Long> {

    Optional<PolicyCategory> findByCodeIgnoreCase(
            String code
    );

    boolean existsByNameIgnoreCase(
            String name
    );

    boolean existsByCodeIgnoreCase(
            String code
    );

    List<PolicyCategory> findByActiveTrueOrderByNameAsc();
}