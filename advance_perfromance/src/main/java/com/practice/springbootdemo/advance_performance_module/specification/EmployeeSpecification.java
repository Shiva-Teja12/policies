package com.practice.springbootdemo.ascend_performance.specification;

import com.practice.springbootdemo.advance_performance_module.DTOs.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.entities.ManagerAssignment;
import com.practice.springbootdemo.advance_performance_module.entities.Role;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<User> filterBy(EmployeeSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), Role.EMPLOYEE));

            if (criteria.getName() != null && !criteria.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + criteria.getName().trim().toLowerCase() + "%"));
            }
            if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().trim().toLowerCase() + "%"));
            }
            if (criteria.getSkill() != null && !criteria.getSkill().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("skill")), "%" + criteria.getSkill().trim().toLowerCase() + "%"));
            }
            if (criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + criteria.getLocation().trim().toLowerCase() + "%"));
            }
            if (criteria.getDomain() != null && !criteria.getDomain().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("domain")), "%" + criteria.getDomain().trim().toLowerCase() + "%"));
            }
            if (criteria.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("departmentId"), criteria.getDepartmentId()));
            }
            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }
            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), criteria.getMinExperience()));
            }
            if (criteria.getMaxExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("experienceYears"), criteria.getMaxExperience()));
            }
            if (criteria.getManagerId() != null && query != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ManagerAssignment> assignmentRoot = subquery.from(ManagerAssignment.class);
                subquery.select(assignmentRoot.get("employeeId"))
                        .where(cb.equal(assignmentRoot.get("managerId"), criteria.getManagerId()), cb.isTrue(assignmentRoot.get("active")));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}