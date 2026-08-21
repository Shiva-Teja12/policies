package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.DTOs.auth.LoginRequest;
import com.practice.springbootdemo.advance_performance_module.entities.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;
    public AuthService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ManagerAssignmentRepository assignmentRepository,
            PerformanceCycleRepository cycleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.cycleRepository = cycleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.departmentId()));
        String employeeCode = generateEmployeeCode();
        User user = User.builder()
                .employeeCode(employeeCode)
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.EMPLOYEE)
                .departmentId(department.getId())
                .active(true)
                .build();
        User saved = userRepository.save(user);
        log.info("Registered new employee: ID={}, Code={}, Email={}", saved.getId(), saved.getEmployeeCode(), saved.getEmail());
        if (department.getDefaultManagerId() != null) {
            Long activeCycleId = cycleRepository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE)
                    .map(PerformanceCycle::getId)
                    .orElse(null);
            ManagerAssignment assignment = ManagerAssignment.builder()
                    .employeeId(saved.getId())
                    .managerId(department.getDefaultManagerId())
                    .performanceCycleId(activeCycleId)
                    .active(true)
                    .build();
            assignmentRepository.save(assignment);
            log.info("Auto-assigned employee {} to department default manager {}", saved.getId(), department.getDefaultManagerId());
        }
        return new SignupResponse(
                saved.getId(),
                saved.getEmployeeCode(),
                saved.getName(),
                saved.getEmail(),
                department.getId(),
                department.getName(),
                "Account created successfully"
        );
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!user.isActive()) {
            throw new UnauthorizedException("User account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        String token = jwtService.generate(user);
        String departmentName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }
        log.info("User login successful: ID={}, Role={}, Email={}", user.getId(), user.getRole(), user.getEmail());
        return new LoginResponse(
                token,
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                departmentName
        );
    }
    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        String departmentName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }
        return new LoginResponse(
                null,
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                departmentName
        );
    }
    private String generateEmployeeCode() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('employee_code_seq')", Long.class);
        return String.format("EMP%03d", nextVal != null ? nextVal : System.currentTimeMillis() % 1000);
    }