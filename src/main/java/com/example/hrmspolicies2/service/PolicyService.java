package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.exception.BadRequestException;
import com.example.hrmspolicies2.exception.DuplicateResourceException;
import com.example.hrmspolicies2.exception.ResourceNotFoundException;
import com.example.hrmspolicies2.repository.PolicyRepository;
import com.example.hrmspolicies2.repository.UserRepository;
import com.example.hrmspolicies2.specification.PolicySpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "code", "category", "applicability", "mandatory", "status", "createdAt", "updatedAt"
    );

    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;

    public PolicyService(
            PolicyRepository policyRepository,
            UserRepository userRepository
    ) {
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // GET ALL (simple, unpaginated - kept for
    // backwards compatibility with existing clients)
    // ==========================================

    public List<Policy> getAllPolicies() {
        log.info("Fetching all policies");

        return policyRepository.findAll();
    }

    // ==========================================
    // GET BY ID
    // ==========================================

    public Policy getPolicyById(Long id) {
        return policyRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.warn("Policy lookup failed - id {} does not exist", id);
                    return ResourceNotFoundException.forEntity("Policy", id);
                });
    }

    // ==========================================
    // ADVANCED SEARCH: filtering + sorting + pagination
    // ==========================================

    public PageResponse<Policy> searchPolicies(
            String keyword,
            String category,
            String status,
            String applicability,
            Boolean mandatory,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";

        if (!SORTABLE_FIELDS.contains(sortField)) {
            throw new BadRequestException(
                    "Invalid sortBy field '" + sortField + "'. Allowed values: " + SORTABLE_FIELDS
            );
        }

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size < 1 ? 10 : size,
                Sort.by(sortDirection, sortField)
        );

        log.info(
                "Searching policies [keyword={}, category={}, status={}, applicability={}, mandatory={}, page={}, size={}, sortBy={}, direction={}]",
                keyword, category, status, applicability, mandatory, page, size, sortField, sortDirection
        );

        Page<Policy> result = policyRepository.findAll(
                PolicySpecification.filterBy(keyword, category, status, applicability, mandatory),
                pageable
        );

        return new PageResponse<>(result);
    }

    // ==========================================
    // CREATE
    // ==========================================

    public Policy createPolicy(PolicyRequest request) {
        if (policyRepository.existsByCode(request.getCode())) {
            log.warn("Attempted to create policy with duplicate code {}", request.getCode());
            throw new DuplicateResourceException("Policy code already exists: " + request.getCode());
        }

        Policy policy = Policy.builder()
                .name(request.getName())
                .code(request.getCode())
                .category(request.getCategory())
                .content(request.getContent())
                .applicability(request.getApplicability())
                .mandatory(request.getMandatory())
                .status(request.getStatus() == null ? "DRAFT" : request.getStatus())
                .createdBy(currentUser().orElse(null))
                .build();

        Policy saved = policyRepository.save(policy);

        log.info("Created policy id={} code={}", saved.getId(), saved.getCode());

        return saved;
    }

    // ==========================================
    // FULL UPDATE
    // ==========================================

    public Policy updatePolicy(Long id, PolicyRequest request) {
        Policy policy = getPolicyById(id);

        if (!policy.getCode().equalsIgnoreCase(request.getCode())
                && policyRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Policy code already exists: " + request.getCode());
        }

        policy.setName(request.getName());
        policy.setCode(request.getCode());
        policy.setCategory(request.getCategory());
        policy.setContent(request.getContent());
        policy.setApplicability(request.getApplicability());
        policy.setMandatory(request.getMandatory());
        policy.setStatus(request.getStatus());

        Policy saved = policyRepository.save(policy);

        log.info("Updated policy id={}", saved.getId());

        return saved;
    }

    // ==========================================
    // PATCH
    // ==========================================

    public Policy patchPolicy(Long id, PolicyRequest request) {
        Policy policy = getPolicyById(id);

        if (request.getName() != null) {
            policy.setName(request.getName());
        }

        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(policy.getCode())) {
            if (policyRepository.existsByCode(request.getCode())) {
                throw new DuplicateResourceException("Policy code already exists: " + request.getCode());
            }
            policy.setCode(request.getCode());
        }

        if (request.getCategory() != null) {
            policy.setCategory(request.getCategory());
        }

        if (request.getContent() != null) {
            policy.setContent(request.getContent());
        }

        if (request.getApplicability() != null) {
            policy.setApplicability(request.getApplicability());
        }

        if (request.getMandatory() != null) {
            policy.setMandatory(request.getMandatory());
        }

        if (request.getStatus() != null) {
            policy.setStatus(request.getStatus());
        }

        Policy saved = policyRepository.save(policy);

        log.info("Patched policy id={}", saved.getId());

        return saved;
    }

    // ==========================================
    // DELETE
    // ==========================================

    public void deletePolicy(Long id) {
        Policy policy = getPolicyById(id);

        policyRepository.delete(policy);

        log.info("Deleted policy id={}", id);
    }

    // ==========================================
    // HELPER: resolve the logged-in user (if any)
    // from the security context set by JwtAuthenticationFilter
    // ==========================================

    private java.util.Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof String email) || "anonymousUser".equals(email)) {
            return java.util.Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}
