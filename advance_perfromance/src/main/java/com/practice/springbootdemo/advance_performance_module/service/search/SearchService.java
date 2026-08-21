package com.practice.springbootdemo.advance_performance_module.service.search;

import com.practice.springbootdemo.advance_performance_module.DTOs.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.entities.Goal;
import com.practice.springbootdemo.advance_performance_module.entities.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.specification.GoalSpecification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SearchService {
    private static final Set<String> ALLOWED_EMPLOYEE_SORT_FIELDS = Set.of(
            "id", "name", "email", "employeeCode", "experienceYears", "location", "skill", "domain", "createdAt"
    );
    private static final Set<String> ALLOWED_GOAL_SORT_FIELDS = Set.of(
            "id", "title", "dueDate", "weight", "progress", "status", "createdAt", "updatedAt"
    );
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;
    public SearchService(
            UserRepository userRepository,
            GoalRepository goalRepository,
            ManagerAssignmentRepository assignmentRepository,
            DepartmentRepository departmentRepository
    ) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.assignmentRepository = assignmentRepository;
        this.departmentRepository = departmentRepository;
    }
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(EmployeeSearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_EMPLOYEE_SORT_FIELDS,
                "name"
        );
        Page<User> userPage = userRepository.findAll(com.practice.springbootdemo.ascend_performance.specification.EmployeeSpecification.filterBy(criteria), pageable);
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) {
            log.warn("Slow employee search query detected! Duration: {}ms", duration);
        }
        Map<Long, String> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        Page<EmployeeResponse> dtoPage = userPage.map(u -> new EmployeeResponse(
                u.getId(),
                u.getEmployeeCode(),
                u.getName(),
                u.getEmail(),
                u.getDepartmentId(),
                u.getDepartmentId() != null ? deptMap.getOrDefault(u.getDepartmentId(), "-") : "-",
                u.getRole(),
                u.getSkill(),
                u.getLocation(),
                u.getDomain(),
                u.getExperienceYears(),
                u.isActive()
        ));
        return PagedResponse.from(dtoPage);
    }
    @Transactional(readOnly = true)
    public PagedResponse<GoalResponse> searchManagerGoals(GoalSearchCriteria criteria, Long managerId) {
        long startTime = System.currentTimeMillis();
        List<Long> assignedEmployeeIds = assignmentRepository.findByManagerIdAndActiveTrue(managerId).stream()
                .map(ManagerAssignment::getEmployeeId)
                .toList();
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_GOAL_SORT_FIELDS,
                "dueDate"
        );
        Page<Goal> goalPage = goalRepository.findAll(GoalSpecification.filterBy(criteria, assignedEmployeeIds), pageable);
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) {
            log.warn("Slow manager goal search query detected! Duration: {}ms", duration);
        }
        Map<Long, String> userNames = userRepository.findAllById(
                goalPage.getContent().stream().flatMap(g -> java.util.stream.Stream.of(g.getEmployeeId(), g.getManagerId())).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getName));
        Page<GoalResponse> dtoPage = goalPage.map(g -> new GoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getEmployeeId(),
                userNames.getOrDefault(g.getEmployeeId(), "Unknown"),
                g.getManagerId(),
                userNames.getOrDefault(g.getManagerId(), "Unknown"),
                g.getGoalType(),
                g.getGoalScope(),
                g.getParentGoalId(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isModificationRequested(),
                g.isEmployeeAccepted(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        ));
        return PagedResponse.from(dtoPage);
    }
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeGoalResponse> searchEmployeeGoals(GoalSearchCriteria criteria, Long employeeId) {
        criteria.setEmployeeId(employeeId);
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_GOAL_SORT_FIELDS,
                "dueDate"
        );
        Page<Goal> goalPage = goalRepository.findAll(GoalSpecification.filterBy(criteria, List.of(employeeId)), pageable);
        Page<EmployeeGoalResponse> dtoPage = goalPage.map(g -> new EmployeeGoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getGoalType(),
                g.getGoalScope(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isEmployeeAccepted(),
                g.isModificationRequested(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        ));
        return PagedResponse.from(dtoPage);
    }
    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction,
            Set<String> allowedSortFields,
            String defaultSortField
    ) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be negative (Zero-based pagination: page >= 0)");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : defaultSortField;
        if (!allowedSortFields.contains(sortProperty)) {
            throw new BadRequestException("Invalid sort field: '" + sortProperty + "'. Allowed fields: " + allowedSortFields);
        }
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (direction != null && !direction.isBlank()) {
            if ("desc".equalsIgnoreCase(direction.trim())) {
                sortDirection = Sort.Direction.DESC;
            } else if (!"asc".equalsIgnoreCase(direction.trim())) {
                throw new BadRequestException("Invalid sort direction: '" + direction + "'. Allowed: 'asc', 'desc'");
            }
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}