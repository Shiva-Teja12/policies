package com.practice.springbootdemo.advance_performance_module.DTOs.hr;


import com.practice.springbootdemo.advance_performance_module.entities.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee profile and department details response")
public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        Role role,
        String skill,
        String location,
        String domain,
        Integer experienceYears,
        boolean active
) {
    public EmployeeResponse(Long id, String employeeCode, String name, String email, Long departmentId, Role role) {
        this(id, employeeCode, name, email, departmentId, null, role, null, null, null, null, true);
    }
}