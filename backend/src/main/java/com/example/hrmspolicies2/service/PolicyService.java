package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PolicyRequest;
import com.example.hrmspolicies2.dto.response.PageResponse;
import com.example.hrmspolicies2.dto.response.PolicyResponse;
import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.PolicyCategory;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.enums.Applicability;
import com.example.hrmspolicies2.enums.PolicyStatus;
import com.example.hrmspolicies2.exception.*;
import com.example.hrmspolicies2.repository.*;
import com.example.hrmspolicies2.specification.PolicySpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyService.class
            );

    private static final Set<String> SORTABLE_FIELDS =
            Set.of(
                    "id",
                    "name",
                    "code",
                    "status",
                    "applicability",
                    "mandatory",
                    "effectiveDate",
                    "createdAt",
                    "updatedAt"
            );

    private final PolicyRepository policyRepository;
    private final PolicyCategoryRepository categoryRepository;
    private final UserRepository userRepository;


    public PolicyService(
            PolicyRepository policyRepository,
            PolicyCategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.policyRepository = policyRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // SEARCH / FILTER / PAGINATION / SORTING
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> searchPolicies(
            String search,
            Long categoryId,
            PolicyStatus status,
            Applicability applicability,
            Boolean mandatory,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        log.debug(
                "event=POLICY_SEARCH_REQUEST categoryId={} status={} applicability={} mandatory={} page={} size={} sortBy={} direction={} searchPresent={}",
                categoryId,
                status,
                applicability,
                mandatory,
                page,
                size,
                sortBy,
                direction,
                StringUtils.hasText(search)
        );

        String safeSortField =
                StringUtils.hasText(sortBy)
                        ? sortBy
                        : "updatedAt";

        if (!SORTABLE_FIELDS.contains(safeSortField)) {

            log.warn(
                    "event=POLICY_SEARCH_REJECTED reason=INVALID_SORT_FIELD sortField={}",
                    safeSortField
            );

            throw new BadRequestException(
                    "Invalid sorting field: "
                            + safeSortField
            );
        }

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                sortDirection,
                                safeSortField
                        )
                );

        Page<Policy> policies =
                policyRepository.findAll(
                        PolicySpecification.filterBy(
                                search,
                                categoryId,
                                status,
                                applicability,
                                mandatory,
                                isManagementUser()
                        ),
                        pageable
                );

        Page<PolicyResponse> responses =
                policies.map(
                        this::mapToResponse
                );

        log.debug(
                "event=POLICY_SEARCH_COMPLETED page={} size={} returned={} totalElements={} totalPages={}",
                responses.getNumber(),
                responses.getSize(),
                responses.getNumberOfElements(),
                responses.getTotalElements(),
                responses.getTotalPages()
        );

        return new PageResponse<>(
                responses
        );
    }


    // =========================================================
    // GET POLICY BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(
            Long id
    ) {

        log.debug(
                "event=POLICY_GET_REQUEST policyId={}",
                id
        );

        Policy policy =
                findPolicy(id);

        /*
         * Non-management users should only
         * access published policies.
         */
        if (!isManagementUser()
                && policy.getStatus()
                != PolicyStatus.PUBLISHED) {

            log.warn(
                    "event=POLICY_ACCESS_DENIED policyId={} status={} reason=NOT_PUBLISHED",
                    id,
                    policy.getStatus()
            );

            throw new ForbiddenException(
                    "You cannot access this policy"
            );
        }

        log.debug(
                "event=POLICY_GET_SUCCESS policyId={} status={}",
                policy.getId(),
                policy.getStatus()
        );

        return mapToResponse(
                policy
        );
    }


    // =========================================================
    // CREATE POLICY
    // =========================================================

    @Transactional
    public PolicyResponse createPolicy(
            PolicyRequest request
    ) {

        String code =
                normalizeCode(
                        request.getCode()
                );

        log.info(
                "event=POLICY_CREATE_REQUEST code={} applicability={} mandatory={}",
                code,
                request.getApplicability(),
                request.getMandatory()
        );

        validateApplicability(
                request
        );

        if (policyRepository
                .existsByCodeIgnoreCase(code)) {

            log.warn(
                    "event=POLICY_CREATE_REJECTED reason=DUPLICATE_CODE code={}",
                    code
            );

            throw new DuplicateResourceException(
                    "Policy code already exists: "
                            + code
            );
        }

        PolicyCategory category =
                findCategory(
                        request.getCategoryId()
                );

        User currentUser =
                getCurrentUser();

        Policy policy =
                Policy.builder()

                        .name(
                                request.getName()
                                        .trim()
                        )

                        .code(
                                code
                        )

                        .category(
                                category
                        )

                        .content(
                                request.getContent()
                                        .trim()
                        )

                        .applicability(
                                request.getApplicability()
                        )

                        .applicableDepartments(
                                joinValues(
                                        request
                                                .getApplicableDepartments()
                                )
                        )

                        .applicableGrades(
                                joinValues(
                                        request
                                                .getApplicableGrades()
                                )
                        )

                        .mandatory(
                                request.getMandatory()
                        )

                        .status(
                                PolicyStatus.DRAFT
                        )

                        .acknowledgementPeriodDays(
                                defaultDays(
                                        request
                                                .getAcknowledgementPeriodDays()
                                )
                        )

                        .onboardingPeriodDays(
                                defaultDays(
                                        request
                                                .getOnboardingPeriodDays()
                                )
                        )

                        .createdBy(
                                currentUser
                        )

                        .build();

        Policy saved =
                policyRepository.save(
                        policy
                );

        log.info(
                "event=POLICY_CREATED policyId={} code={} status={} actorUserId={}",
                saved.getId(),
                saved.getCode(),
                saved.getStatus(),
                currentUser.getId()
        );

        return mapToResponse(
                saved
        );
    }


    // =========================================================
    // UPDATE POLICY
    // =========================================================

    @Transactional
    public PolicyResponse updatePolicy(
            Long id,
            PolicyRequest request
    ) {

        log.info(
                "event=POLICY_UPDATE_REQUEST policyId={}",
                id
        );

        Policy policy =
                findPolicy(id);

        /*
         * HR can edit:
         *
         * DRAFT
         * REJECTED
         */
        if (policy.getStatus()
                != PolicyStatus.DRAFT
                &&
                policy.getStatus()
                        != PolicyStatus.REJECTED) {

            log.warn(
                    "event=POLICY_UPDATE_REJECTED policyId={} reason=INVALID_STATUS status={}",
                    id,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only DRAFT or REJECTED policies can be edited"
            );
        }

        validateApplicability(
                request
        );

        String requestedCode =
                normalizeCode(
                        request.getCode()
                );

        /*
         * Once a policy has been published,
         * its code should remain permanent.
         */
        if (Boolean.TRUE.equals(
                policy.getPublishedOnce()
        )
                &&
                !policy.getCode()
                        .equals(requestedCode)) {

            log.warn(
                    "event=POLICY_UPDATE_REJECTED policyId={} reason=CODE_CHANGE_AFTER_PUBLICATION oldCode={} requestedCode={}",
                    id,
                    policy.getCode(),
                    requestedCode
            );

            throw new BadRequestException(
                    "Policy code cannot be changed after first publication"
            );
        }

        if (policyRepository
                .existsByCodeIgnoreCaseAndIdNot(
                        requestedCode,
                        id
                )) {

            log.warn(
                    "event=POLICY_UPDATE_REJECTED policyId={} reason=DUPLICATE_CODE requestedCode={}",
                    id,
                    requestedCode
            );

            throw new DuplicateResourceException(
                    "Policy code already exists: "
                            + requestedCode
            );
        }

        PolicyCategory category =
                findCategory(
                        request.getCategoryId()
                );

        policy.setName(
                request.getName()
                        .trim()
        );

        policy.setCode(
                requestedCode
        );

        policy.setCategory(
                category
        );

        policy.setContent(
                request.getContent()
                        .trim()
        );

        policy.setApplicability(
                request.getApplicability()
        );

        policy.setApplicableDepartments(
                joinValues(
                        request
                                .getApplicableDepartments()
                )
        );

        policy.setApplicableGrades(
                joinValues(
                        request
                                .getApplicableGrades()
                )
        );

        policy.setMandatory(
                request.getMandatory()
        );

        policy.setAcknowledgementPeriodDays(
                defaultDays(
                        request
                                .getAcknowledgementPeriodDays()
                )
        );

        policy.setOnboardingPeriodDays(
                defaultDays(
                        request
                                .getOnboardingPeriodDays()
                )
        );

        /*
         * If HR edits a rejected policy,
         * return it to DRAFT.
         */
        if (policy.getStatus()
                == PolicyStatus.REJECTED) {

            log.info(
                    "event=POLICY_REJECTED_RESET_TO_DRAFT policyId={}",
                    id
            );

            policy.setStatus(
                    PolicyStatus.DRAFT
            );
        }

        Policy saved =
                policyRepository.save(
                        policy
                );

        log.info(
                "event=POLICY_UPDATED policyId={} code={} status={}",
                saved.getId(),
                saved.getCode(),
                saved.getStatus()
        );

        return mapToResponse(
                saved
        );
    }


    // =========================================================
    // DELETE POLICY
    // =========================================================

    @Transactional
    public void deletePolicy(
            Long id
    ) {

        log.info(
                "event=POLICY_DELETE_REQUEST policyId={}",
                id
        );

        Policy policy =
                findPolicy(id);

        /*
         * Only an actual DRAFT policy may be
         * physically deleted.
         *
         * Policies that entered workflow should
         * remain for audit/history purposes.
         */
        if (policy.getStatus()
                != PolicyStatus.DRAFT) {

            log.warn(
                    "event=POLICY_DELETE_REJECTED policyId={} reason=INVALID_STATUS status={}",
                    id,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only DRAFT policies can be deleted. " +
                            "Reviewed, approved, published or retired " +
                            "policies must be preserved."
            );
        }

        /*
         * A policy that has been published at least
         * once must never be physically deleted.
         */
        if (Boolean.TRUE.equals(
                policy.getPublishedOnce()
        )) {

            log.warn(
                    "event=POLICY_DELETE_REJECTED policyId={} reason=PUBLICATION_HISTORY",
                    id
            );

            throw new BadRequestException(
                    "A policy with publication history " +
                            "cannot be deleted. Use Retire Policy instead."
            );
        }

        try {

            /*
             * Delete the policy.
             */
            policyRepository.delete(
                    policy
            );

            /*
             * IMPORTANT:
             *
             * Hibernate normally waits until the transaction
             * finishes before sending the DELETE SQL.
             *
             * flush() forces PostgreSQL to execute it here.
             *
             * If a notification, approval, version or other
             * table still references the policy, PostgreSQL
             * throws the constraint exception here and we
             * convert it into a clean BadRequestException.
             */
            policyRepository.flush();

            log.info(
                    "event=POLICY_DELETED policyId={} code={}",
                    id,
                    policy.getCode()
            );

        } catch (
                DataIntegrityViolationException exception
        ) {

            log.error(
                    "event=POLICY_DELETE_FAILED policyId={} reason=DATA_INTEGRITY",
                    id,
                    exception
            );

            throw new BadRequestException(
                    "This policy cannot be deleted because " +
                            "it already has related workflow or history records. " +
                            "Preserve the policy instead of deleting it."
            );
        }
    }


    // =========================================================
    // FIND POLICY
    // =========================================================

    private Policy findPolicy(
            Long id
    ) {

        return policyRepository
                .findById(id)
                .orElseThrow(
                        () -> {

                            log.warn(
                                    "event=POLICY_NOT_FOUND policyId={}",
                                    id
                            );

                            return ResourceNotFoundException
                                    .forEntity(
                                            "Policy",
                                            id
                                    );
                        }
                );
    }


    // =========================================================
    // FIND CATEGORY
    // =========================================================

    private PolicyCategory findCategory(
            Long categoryId
    ) {

        PolicyCategory category =
                categoryRepository
                        .findById(
                                categoryId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_CATEGORY_NOT_FOUND categoryId={}",
                                            categoryId
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "PolicyCategory",
                                                    categoryId
                                            );
                                }
                        );

        if (!Boolean.TRUE.equals(
                category.getActive()
        )) {

            log.warn(
                    "event=POLICY_CATEGORY_REJECTED categoryId={} reason=INACTIVE_CATEGORY",
                    categoryId
            );

            throw new BadRequestException(
                    "Selected policy category is inactive"
            );
        }

        return category;
    }


    // =========================================================
    // VALIDATE APPLICABILITY
    // =========================================================

    private void validateApplicability(
            PolicyRequest request
    ) {

        /*
         * Department based policy must contain
         * at least one department.
         */
        if (request.getApplicability()
                == Applicability.DEPT_BASED
                &&
                isEmpty(
                        request
                                .getApplicableDepartments()
                )) {

            log.warn(
                    "event=POLICY_VALIDATION_FAILED reason=MISSING_DEPARTMENT applicability={}",
                    request.getApplicability()
            );

            throw new BadRequestException(
                    "Select at least one department " +
                            "for DEPT_BASED applicability"
            );
        }

        /*
         * Grade based policy must contain
         * at least one grade.
         */
        if (request.getApplicability()
                == Applicability.GRADE_BASED
                &&
                isEmpty(
                        request
                                .getApplicableGrades()
                )) {

            log.warn(
                    "event=POLICY_VALIDATION_FAILED reason=MISSING_GRADE applicability={}",
                    request.getApplicability()
            );

            throw new BadRequestException(
                    "Select at least one grade " +
                            "for GRADE_BASED applicability"
            );
        }
    }


    // =========================================================
    // CHECK EMPTY LIST
    // =========================================================

    private boolean isEmpty(
            List<String> values
    ) {

        return values == null
                ||
                values.stream()
                        .noneMatch(
                                StringUtils::hasText
                        );
    }


    // =========================================================
    // CONVERT LIST TO DATABASE STRING
    // =========================================================

    private String joinValues(
            List<String> values
    ) {

        if (values == null) {
            return null;
        }

        String joined =
                values.stream()

                        .filter(
                                StringUtils::hasText
                        )

                        .map(
                                String::trim
                        )

                        .distinct()

                        .collect(
                                Collectors.joining(",")
                        );

        return joined.isBlank()
                ? null
                : joined;
    }


    // =========================================================
    // CONVERT DATABASE STRING TO LIST
    // =========================================================

    private List<String> splitValues(
            String values
    ) {

        if (!StringUtils.hasText(values)) {
            return List.of();
        }

        return Arrays.stream(
                        values.split(",")
                )

                .map(
                        String::trim
                )

                .filter(
                        StringUtils::hasText
                )

                .toList();
    }


    // =========================================================
    // DEFAULT ACKNOWLEDGEMENT DAYS
    // =========================================================

    private int defaultDays(
            Integer days
    ) {

        return days == null
                ? 7
                : days;
    }


    // =========================================================
    // NORMALIZE POLICY CODE
    // =========================================================

    private String normalizeCode(
            String code
    ) {

        if (!StringUtils.hasText(code)) {

            log.warn(
                    "event=POLICY_VALIDATION_FAILED reason=MISSING_POLICY_CODE"
            );

            throw new BadRequestException(
                    "Policy code is required"
            );
        }

        return code
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // GET CURRENT AUTHENTICATED USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                ||
                !authentication
                        .isAuthenticated()) {

            log.warn(
                    "event=POLICY_AUTHENTICATION_FAILED reason=NO_AUTHENTICATED_USER"
            );

            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }

        return userRepository
                .findByEmailIgnoreCase(
                        authentication.getName()
                )
                .orElseThrow(
                        () -> {

                            log.warn(
                                    "event=POLICY_AUTHENTICATION_FAILED reason=USER_NOT_FOUND"
                            );

                            return new UnauthorizedException(
                                    "Authenticated user was not found"
                            );
                        }
                );
    }


    // =========================================================
    // CHECK WHETHER USER IS MANAGEMENT
    // =========================================================

    private boolean isManagementUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                Set.of(
                                                "ROLE_HR_ADMIN",
                                                "ROLE_LEGAL_REVIEWER",
                                                "ROLE_HR_HEAD",
                                                "ROLE_MANAGING_DIRECTOR"
                                        )
                                        .contains(
                                                authority
                                                        .getAuthority()
                                        )
                );
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private PolicyResponse mapToResponse(
            Policy policy
    ) {

        return PolicyResponse
                .builder()

                .id(
                        policy.getId()
                )

                .name(
                        policy.getName()
                )

                .code(
                        policy.getCode()
                )

                .categoryId(
                        policy.getCategory()
                                .getId()
                )

                .categoryName(
                        policy.getCategory()
                                .getName()
                )

                .categoryCode(
                        policy.getCategory()
                                .getCode()
                )

                .content(
                        policy.getContent()
                )

                .applicability(
                        policy.getApplicability()
                )

                .applicableDepartments(
                        splitValues(
                                policy.getApplicableDepartments()
                        )
                )

                .applicableGrades(
                        splitValues(
                                policy.getApplicableGrades()
                        )
                )

                .mandatory(
                        policy.getMandatory()
                )

                .status(
                        policy.getStatus()
                )

                .acknowledgementPeriodDays(
                        policy
                                .getAcknowledgementPeriodDays()
                )

                .onboardingPeriodDays(
                        policy
                                .getOnboardingPeriodDays()
                )

                .publishedOnce(
                        policy.getPublishedOnce()
                )

                .effectiveDate(
                        policy.getEffectiveDate()
                )

                .createdById(
                        policy.getCreatedBy()
                                == null
                                ? null
                                : policy
                                .getCreatedBy()
                                .getId()
                )

                .createdByName(
                        policy.getCreatedBy()
                                == null
                                ? null
                                : policy
                                .getCreatedBy()
                                .getName()
                )

                .createdAt(
                        policy.getCreatedAt()
                )

                .updatedAt(
                        policy.getUpdatedAt()
                )

                .build();
    }
}