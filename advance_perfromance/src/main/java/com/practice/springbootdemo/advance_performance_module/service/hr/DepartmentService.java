package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.DTOs.hr.DepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;

@Slf4j
@Service
public class DepartmentService {
    private final DepartmentRepository repository;
    private final UserRepository userRepository;
    public DepartmentService(DepartmentRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("Department already exists: " + request.name());
        }
        Department department = new Department();
        department.setName(request.name().trim());
        department.setDescription(request.description());
        Department saved = repository.save(department);
        log.info("Created department: ID={}, Name={}", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }
    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<PublicDepartmentResponse> listPublic() {
        return repository.findAll().stream()
                .map(d -> new PublicDepartmentResponse(d.getId(), d.getName()))
                .toList();
    }
    @Transactional
    public DepartmentResponse setDefaultManager(Long departmentId, Long managerId) {
        Department department = repository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("User ID " + managerId + " does not have MANAGER role");
        }
        department.setDefaultManagerId(manager.getId());
        Department saved = repository.save(department);
        log.info("Set default manager {} for department {}", managerId, departmentId);
        return mapToResponse(saved);
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDepartmentEmployees(Long departmentId) {
        return userRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .map(u -> new EmployeeResponse(
                        u.getId(),
                        u.getEmployeeCode(),
                        u.getName(),
                        u.getEmail(),
                        u.getDepartmentId(),
                        u.getRole()
                ))
                .toList();
    }
    private DepartmentResponse mapToResponse(Department d) {
        String defaultManagerName = null;
        if (d.getDefaultManagerId() != null) {
            defaultManagerName = userRepository.findById(d.getDefaultManagerId()).map(User::getName).orElse(null);
        }
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), d.getDefaultManagerId(), defaultManagerName);
    }