package com.practice.springbootdemo.advance_performance_module.config;

import com.practice.springbootdemo.advance_performance_module.entities.Department;
import com.practice.springbootdemo.advance_performance_module.entities.ManagerAssignment;
import com.practice.springbootdemo.advance_performance_module.entities.Role;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.UserCodeGeneratorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Configuration
@Profile("dev")
public class DevBulkDataSeeder {
    private static final List<String> DEPARTMENTS = List.of(
            "Engineering", "Product", "Sales", "Marketing", "Customer Success", "Finance"
    );
    private static final int EMPLOYEES_PER_DEPARTMENT = 5;
    @Bean
    @Order(10)
    CommandLineRunner bulkSeed(
            UserRepository users,
            DepartmentRepository departments,
            ManagerAssignmentRepository assignments,
            PasswordEncoder encoder,
            UserCodeGeneratorService codeGenerator
    ) {
        return args -> {
            // Only seed if bulk departments do not already exist
            if (departments.existsByNameIgnoreCase(DEPARTMENTS.get(0))) {
                return;
            }
            log.info("Starting development profile bulk data generation...");
            int deptIndex = 0;
            for (String departmentName : DEPARTMENTS) {
                deptIndex++;
                // 1. Create Department
                Department department = new Department();
                department.setName(departmentName);
                department = departments.save(department);
                // 2. Create Department Manager
                String managerEmail = "manager" + deptIndex + "@ascend.local";
                User manager = User.builder()
                        .employeeCode(codeGenerator.generateManagerCode())
                        .name(departmentName + " Manager")
                        .email(managerEmail)
                        .passwordHash(encoder.encode("Password1"))
                        .role(Role.MANAGER)
                        .departmentId(department.getId())
                        .skill("Management, " + departmentName)
                        .domain(departmentName)
                        .location("Hyderabad")
                        .experienceYears(8 + deptIndex)
                        .active(true)
                        .build();
                manager = users.save(manager);
                // Link default manager to department
                department.setDefaultManagerId(manager.getId());
                departments.save(department);
                // 3. Create Employees under this Department and assign to Manager
                for (int i = 1; i <= EMPLOYEES_PER_DEPARTMENT; i++) {
                    String employeeEmail = "employee" + deptIndex + "_" + i + "@ascend.local";
                    User employee = User.builder()
                            .employeeCode(codeGenerator.generateEmployeeCode())
                            .name(departmentName + " Employee " + i)
                            .email(employeeEmail)
                            .passwordHash(encoder.encode("Password1"))
                            .role(Role.EMPLOYEE)
                            .departmentId(department.getId())
                            .skill(i % 2 == 0 ? "Java, Spring Boot" : "React, TypeScript")
                            .domain(departmentName)
                            .location(i % 2 == 0 ? "Hyderabad" : "Bangalore")
                            .experienceYears(2 + i)
                            .active(true)
                            .build();
                    employee = users.save(employee);
                    // Assign Manager -> Employee
                    ManagerAssignment assignment = ManagerAssignment.builder()
                            .employeeId(employee.getId())
                            .managerId(manager.getId())
                            .active(true)
                            .assignedDate(LocalDateTime.now())
                            .build();
                    assignments.save(assignment);
                }
            }
            log.info("Bulk data seeding completed for {} departments with {} employees each.",
                    DEPARTMENTS.size(), EMPLOYEES_PER_DEPARTMENT);
        };
    }
}