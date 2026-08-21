package com.practice.springbootdemo.advance_performance_module.config;

import com.practice.springbootdemo.advance_performance_module.entities.*;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(
            UserRepository users,
            DepartmentRepository departments,
            PerformanceCycleRepository cycles,
            ManagerAssignmentRepository assignments,
            PasswordEncoder encoder,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            log.info("Executing ASCEND database seeder and sequence initialization...");
            // 1. Ensure PostgreSQL sequences exist for employee and manager codes
            try {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS employee_code_seq START WITH 1 INCREMENT BY 1");
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS manager_code_seq START WITH 1 INCREMENT BY 1");
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS hr_code_seq START WITH 1 INCREMENT BY 1");
            } catch (Exception e) {
                log.debug("Sequences already exist or managed by database");
            }
            // 2. Seed Standard Departments
            Department hrDept = seedDepartments(departments);
            Department backendDept = departments.findByNameIgnoreCase("API & Backend Team").orElse(hrDept);
            Department frontendDept = departments.findByNameIgnoreCase("Frontend Team").orElse(hrDept);
            Department aiDept = departments.findByNameIgnoreCase("AI & ML Team").orElse(hrDept);
            // 3. Seed HR Admin
            User hr = findOrCreate(
                    users, "HR001", "HR Admin", "hr@ascend.local",
                    Role.HR, hrDept != null ? hrDept.getId() : 1L,
                    "HR Management", "Corporate", "Hyderabad", 12, encoder
            );
            // 4. Seed Managers
            User manager1 = findOrCreate(
                    users, "MGR001", "Alice Smith", "manager1@ascend.local",
                    Role.MANAGER, backendDept.getId(),
                    "Java, Microservices, Architecture", "Backend", "Hyderabad", 10, encoder
            );
            User manager2 = findOrCreate(
                    users, "MGR002", "Robert Vance", "manager2@ascend.local",
                    Role.MANAGER, aiDept.getId(),
                    "Python, Cloud, Kubernetes", "AI & Cloud", "Bangalore", 8, encoder
            );
            // 5. Seed Employees
            User emp1 = findOrCreate(
                    users, "EMP001", "John Doe", "emp1@ascend.local",
                    Role.EMPLOYEE, backendDept.getId(),
                    "Java, Spring Boot, PostgreSQL", "Backend", "Hyderabad", 4, encoder
            );
            User emp2 = findOrCreate(
                    users, "EMP002", "Emma Watson", "emp2@ascend.local",
                    Role.EMPLOYEE, frontendDept.getId(),
                    "React, TypeScript, CSS", "Frontend", "Hyderabad", 3, encoder
            );
            User emp3 = findOrCreate(
                    users, "EMP003", "David Miller", "emp3@ascend.local",
                    Role.EMPLOYEE, aiDept.getId(),
                    "Python, TensorFlow, PyTorch", "AI & ML", "Bangalore", 5, encoder
            );
            // 6. Seed Performance Cycles
            PerformanceCycle activeCycle = createPerformanceCycleIfNotExists(
                    cycles, "2026 Annual Performance Review",
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    CycleStatus.ACTIVE, hr.getId()
            );
            createPerformanceCycleIfNotExists(
                    cycles, "2027 Q1 Performance Review",
                    LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31),
                    CycleStatus.DRAFT, hr.getId()
            );
            // 7. Seed Manager-to-Employee Assignments (Level 2 Ownership)
            seedAssignmentIfNotExists(assignments, manager1.getId(), emp1.getId(), activeCycle.getId());
            seedAssignmentIfNotExists(assignments, manager1.getId(), emp2.getId(), activeCycle.getId());
            seedAssignmentIfNotExists(assignments, manager2.getId(), emp3.getId(), activeCycle.getId());
            log.info("Database seeding complete. Ready for Swagger / Postman testing.");
        };
    }

    private Department seedDepartments(DepartmentRepository departments) {
        List<String> defaultDeptNames = List.of(
                "Java Team",
                "Python Team",
                "AI & ML Team",
                "DevOps Team",
                "Product Team",
                "Animation Team",
                "HR Team",
                "UI/UX Team",
                "Frontend Team",
                "Database Team",
                "API & Backend Team"
        );
        for (String name : defaultDeptNames) {
            if (!departments.existsByNameIgnoreCase(name)) {
                Department dept = new Department();
                dept.setName(name);
                departments.save(dept);
            }
        }
        return departments.findByNameIgnoreCase("HR Team")
                .orElse(departments.findAll().stream().findFirst().orElse(null));
    }

    private User findOrCreate(
            UserRepository users,
            String code,
            String name,
            String email,
            Role role,
            Long departmentId,
            String skill,
            String domain,
            String location,
            Integer experienceYears,
            PasswordEncoder encoder
    ) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmployeeCode(code);
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(encoder.encode("Password1"));
            user.setRole(role);
            user.setDepartmentId(departmentId);
            user.setSkill(skill);
            user.setDomain(domain);
            user.setLocation(location);
            user.setExperienceYears(experienceYears);
            user.setActive(true);
            return users.save(user);
        });
    }

    private PerformanceCycle createPerformanceCycleIfNotExists(
            PerformanceCycleRepository cycles,
            String name,
            LocalDate start,
            LocalDate end,
            CycleStatus status,
            Long hrUserId
    ) {
        return cycles.findFirstByStatusOrderByStartDateDesc(status).orElseGet(() -> {
            PerformanceCycle cycle = new PerformanceCycle();
            cycle.setName(name);
            cycle.setDescription("Standard organizational review cycle");
            cycle.setStartDate(start);
            cycle.setEndDate(end);
            cycle.setStatus(status);
            cycle.setCreatedBy(hrUserId);
            return cycles.save(cycle);
        });
    }

    private void seedAssignmentIfNotExists(
            ManagerAssignmentRepository assignments,
            Long managerId,
            Long employeeId,
            Long cycleId
    ) {
        if (assignments.findByEmployeeIdAndActiveTrue(employeeId).isEmpty()) {
            ManagerAssignment assignment = new ManagerAssignment();
            assignment.setManagerId(managerId);
            assignment.setEmployeeId(employeeId);
            assignment.setPerformanceCycleId(cycleId);
            assignment.setActive(true);
            assignment.setAssignedDate(LocalDateTime.now());
            assignments.save(assignment);
        }
    }
}