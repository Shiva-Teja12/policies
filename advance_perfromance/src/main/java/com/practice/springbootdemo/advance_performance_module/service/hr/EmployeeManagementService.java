package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.DTOs.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.entities.User;

@Slf4j
@Service
public class EmployeeManagementService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    public EmployeeManagementService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        return userRepository.findByRoleAndActiveTrue(Role.EMPLOYEE).stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getManagers() {
        return userRepository.findByRoleAndActiveTrue(Role.MANAGER).stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional
    public EmployeeResponse createManager(CreateManagerRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.departmentId()));
        String managerCode = generateManagerCode();
        User manager = User.builder()
                .employeeCode(managerCode)
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.MANAGER)
                .departmentId(department.getId())
                .active(true)
                .build();
        User saved = userRepository.save(manager);
        log.info("HR created manager account: ID={}, Code={}, Email={}", saved.getId(), saved.getEmployeeCode(), saved.getEmail());
        return mapToResponse(saved);
    }
    private String generateManagerCode() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('manager_code_seq')", Long.class);
        return String.format("MGR%03d", nextVal != null ? nextVal : System.currentTimeMillis() % 1000);
    }
    private EmployeeResponse mapToResponse(User u) {
        String deptName = (u.getDepartmentId() != null)
                ? departmentRepository.findById(u.getDepartmentId()).map(Department::getName).orElse(null)
                : null;
        return new EmployeeResponse(
                u.getId(),
                u.getEmployeeCode(),
                u.getName(),
                u.getEmail(),
                u.getDepartmentId(),
                deptName,
                u.getRole(),
                u.getSkill(),
                u.getLocation(),
                u.getDomain(),
                u.getExperienceYears(),
                u.isActive()
        );
    }
}
