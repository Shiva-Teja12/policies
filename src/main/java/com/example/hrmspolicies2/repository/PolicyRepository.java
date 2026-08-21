package com.example.hrmspolicies2.repository;

import com.example.hrmspolicies2.entity.Policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor adds findAll(Specification, Pageable) so the
 * service layer can compose dynamic, multi-field filters (see
 * PolicySpecification) together with sorting and pagination in a single
 * query, instead of hand-writing a derived-query method per filter
 * combination.
 */
public interface PolicyRepository extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    boolean existsByCode(String code);
}
