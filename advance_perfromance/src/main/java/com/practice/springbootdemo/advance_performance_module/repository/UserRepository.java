package com.practice.springbootdemo.advance_performance_module.repository;


import com.practice.springbootdemo.advance_performance_module.entities.Role;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmployeeCode(String employeeCode);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleAndActiveTrue(Role role);
    List<User> findByDepartmentIdAndActiveTrue(Long departmentId);
    List<User> findByDepartmentIdAndRoleAndActiveTrue(Long departmentId, Role role);

}
