package com.practice.springbootdemo.advance_performance_module.specification;

import com.practice.springbootdemo.advance_performance_module.entities.Goal;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class GoalSpecification {


    public static Specification<Goal> filterBy(GoalSearchCriteria criteria, Collection<Long> allowedEmployeeIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (allowedEmployeeIds != null) {
                if (allowedEmployeeIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("employeeId").in(allowedEmployeeIds));
                }
            }
            if (criteria.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employeeId"), criteria.getEmployeeId()));
            }
            if (criteria.getManagerId() != null) {
                predicates.add(cb.equal(root.get("managerId"), criteria.getManagerId()));
            }
            if (criteria.getPerformanceCycleId() != null) {
                predicates.add(cb.equal(root.get("cycleId"), criteria.getPerformanceCycleId()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getGoalType() != null) {
                predicates.add(cb.equal(root.get("goalType"), criteria.getGoalType()));
            }
            if (criteria.getTitle() != null && !criteria.getTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + criteria.getTitle().trim().toLowerCase() + "%"));
            }
            if (criteria.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), criteria.getDueDateFrom()));
            }
            if (criteria.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), criteria.getDueDateTo()));
            }
            if (criteria.getMinProgress() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("progress"), criteria.getMinProgress()));
            }
            if (criteria.getMaxProgress() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("progress"), criteria.getMaxProgress()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
