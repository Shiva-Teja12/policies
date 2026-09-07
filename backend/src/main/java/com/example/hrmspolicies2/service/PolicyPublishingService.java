package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PublishPolicyRequest;
import com.example.hrmspolicies2.dto.response.PolicyVersionResponse;
import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.exception.*;
import com.example.hrmspolicies2.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class PolicyPublishingService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyPublishingService.class
            );

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyAssignmentRepository assignmentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


    public PolicyPublishingService(
            PolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyAssignmentRepository assignmentRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // PUBLISH POLICY
    // =========================================================

    @Transactional
    public PolicyVersionResponse publish(
            Long policyId,
            PublishPolicyRequest request
    ) {

        log.info(
                "event=POLICY_PUBLISH_REQUEST policyId={} effectiveDate={}",
                policyId,
                request.getEffectiveDate()
        );

        Policy policy =
                policyRepository
                        .findByIdForUpdate(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_PUBLISH_FAILED policyId={} reason=POLICY_NOT_FOUND",
                                            policyId
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "Policy",
                                                    policyId
                                            );
                                }
                        );

        if (policy.getStatus()
                != PolicyStatus.APPROVED) {

            log.warn(
                    "event=POLICY_PUBLISH_REJECTED policyId={} reason=INVALID_STATUS status={}",
                    policyId,
                    policy.getStatus()
            );

            throw new BadRequestException(
                    "Only APPROVED policies can be published"
            );
        }

        User publisher =
                currentUser();

        log.debug(
                "event=POLICY_PUBLISH_AUTHORIZED policyId={} publisherUserId={} publisherRole={}",
                policyId,
                publisher.getId(),
                publisher.getRole()
        );

        int nextVersion =
                versionRepository
                        .findTopByPolicy_IdOrderByVersionNumberDesc(
                                policyId
                        )
                        .map(
                                version ->
                                        version.getVersionNumber()
                                                + 1
                        )
                        .orElse(
                                1
                        );

        log.debug(
                "event=POLICY_VERSION_NUMBER_RESOLVED policyId={} nextVersion={}",
                policyId,
                nextVersion
        );

        versionRepository
                .findByPolicy_IdAndCurrentVersionTrue(
                        policyId
                )
                .ifPresent(
                        previous -> {

                            log.info(
                                    "event=POLICY_VERSION_ARCHIVING policyId={} versionId={} versionNumber={}",
                                    policyId,
                                    previous.getId(),
                                    previous.getVersionNumber()
                            );

                            previous.setCurrentVersion(
                                    false
                            );

                            previous.setArchived(
                                    true
                            );

                            versionRepository.save(
                                    previous
                            );

                            log.info(
                                    "event=POLICY_VERSION_ARCHIVED policyId={} versionId={} versionNumber={}",
                                    policyId,
                                    previous.getId(),
                                    previous.getVersionNumber()
                            );
                        }
                );

        int acknowledgementDays =
                request.getAcknowledgementPeriodDays()
                        == null
                        ? policy.getAcknowledgementPeriodDays()
                        : request.getAcknowledgementPeriodDays();

        policy.setAcknowledgementPeriodDays(
                acknowledgementDays
        );

        PolicyVersion version =
                PolicyVersion
                        .builder()
                        .policy(
                                policy
                        )
                        .versionNumber(
                                nextVersion
                        )
                        .contentSnapshot(
                                policy.getContent()
                        )
                        .effectiveDate(
                                request.getEffectiveDate()
                        )
                        .publishedAt(
                                LocalDateTime.now(
                                        ZoneOffset.UTC
                                )
                        )
                        .publishedBy(
                                publisher
                        )
                        .changeSummary(
                                clean(
                                        request.getChangeSummary()
                                )
                        )
                        .currentVersion(
                                true
                        )
                        .archived(
                                false
                        )
                        .build();

        PolicyVersion savedVersion =
                versionRepository
                        .saveAndFlush(
                                version
                        );

        log.info(
                "event=POLICY_VERSION_CREATED policyId={} versionId={} versionNumber={} publisherUserId={}",
                policyId,
                savedVersion.getId(),
                savedVersion.getVersionNumber(),
                publisher.getId()
        );

        policy.setStatus(
                PolicyStatus.PUBLISHED
        );

        policy.setPublishedOnce(
                true
        );

        policy.setEffectiveDate(
                request.getEffectiveDate()
        );

        policyRepository.save(
                policy
        );

        int assignedEmployees =
                assignToApplicableEmployees(
                        policy,
                        savedVersion,
                        acknowledgementDays
                );

        log.info(
                "event=POLICY_PUBLISHED policyId={} versionId={} versionNumber={} status={} assignedEmployees={} acknowledgementDays={} publisherUserId={}",
                policy.getId(),
                savedVersion.getId(),
                savedVersion.getVersionNumber(),
                policy.getStatus(),
                assignedEmployees,
                acknowledgementDays,
                publisher.getId()
        );

        return map(
                savedVersion,
                assignedEmployees
        );
    }


    // =========================================================
    // GET POLICY VERSIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getVersions(
            Long policyId
    ) {

        log.debug(
                "event=POLICY_VERSIONS_REQUEST policyId={}",
                policyId
        );

        Policy policy =
                policyRepository
                        .findById(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_VERSIONS_FAILED policyId={} reason=POLICY_NOT_FOUND",
                                            policyId
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "Policy",
                                                    policyId
                                            );
                                }
                        );

        validateVersionAccess(
                policy
        );

        List<PolicyVersionResponse> versions =
                versionRepository
                        .findByPolicy_IdOrderByVersionNumberDesc(
                                policyId
                        )
                        .stream()
                        .map(
                                version ->
                                        map(
                                                version,
                                                Math.toIntExact(
                                                        assignmentRepository
                                                                .countByPolicyVersion_Id(
                                                                        version.getId()
                                                                )
                                                )
                                        )
                        )
                        .toList();

        log.debug(
                "event=POLICY_VERSIONS_COMPLETED policyId={} versionCount={}",
                policyId,
                versions.size()
        );

        return versions;
    }


    // =========================================================
    // GET SPECIFIC POLICY VERSION
    // =========================================================

    @Transactional(readOnly = true)
    public PolicyVersionResponse getVersion(
            Long policyId,
            Integer versionNumber
    ) {

        log.debug(
                "event=POLICY_VERSION_REQUEST policyId={} versionNumber={}",
                policyId,
                versionNumber
        );

        Policy policy =
                policyRepository
                        .findById(
                                policyId
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_VERSION_FAILED policyId={} versionNumber={} reason=POLICY_NOT_FOUND",
                                            policyId,
                                            versionNumber
                                    );

                                    return ResourceNotFoundException
                                            .forEntity(
                                                    "Policy",
                                                    policyId
                                            );
                                }
                        );

        validateVersionAccess(
                policy
        );

        PolicyVersion version =
                versionRepository
                        .findByPolicy_IdAndVersionNumber(
                                policyId,
                                versionNumber
                        )
                        .orElseThrow(
                                () -> {

                                    log.warn(
                                            "event=POLICY_VERSION_FAILED policyId={} versionNumber={} reason=VERSION_NOT_FOUND",
                                            policyId,
                                            versionNumber
                                    );

                                    return new ResourceNotFoundException(
                                            "Policy version was not found"
                                    );
                                }
                        );

        int assignedEmployees =
                Math.toIntExact(
                        assignmentRepository
                                .countByPolicyVersion_Id(
                                        version.getId()
                                )
                );

        log.debug(
                "event=POLICY_VERSION_COMPLETED policyId={} versionId={} versionNumber={} assignedEmployees={}",
                policyId,
                version.getId(),
                versionNumber,
                assignedEmployees
        );

        return map(
                version,
                assignedEmployees
        );
    }


    // =========================================================
    // ASSIGN POLICY TO APPLICABLE EMPLOYEES
    // =========================================================

    private int assignToApplicableEmployees(
            Policy policy,
            PolicyVersion version,
            int acknowledgementDays
    ) {

        log.info(
                "event=POLICY_ASSIGNMENT_START policyId={} versionId={} applicability={} acknowledgementDays={}",
                policy.getId(),
                version.getId(),
                policy.getApplicability(),
                acknowledgementDays
        );

        List<User> employees =
                userRepository
                        .findByRole(
                                Role.EMPLOYEE
                        );

        LocalDate deadline =
                LocalDate
                        .now(
                                ZoneOffset.UTC
                        )
                        .plusDays(
                                acknowledgementDays
                        );

        int assignedCount =
                0;

        int skippedNotApplicable =
                0;

        int skippedExistingAssignment =
                0;

        for (User employee : employees) {

            if (!isApplicable(
                    policy,
                    employee
            )) {

                skippedNotApplicable++;

                continue;
            }

            if (assignmentRepository
                    .existsByEmployee_IdAndPolicyVersion_Id(
                            employee.getId(),
                            version.getId()
                    )) {

                skippedExistingAssignment++;

                continue;
            }

            PolicyAssignment assignment =
                    PolicyAssignment
                            .builder()
                            .employee(
                                    employee
                            )
                            .policy(
                                    policy
                            )
                            .policyVersion(
                                    version
                            )
                            .deadline(
                                    deadline
                            )
                            .status(
                                    AssignmentStatus.PENDING
                            )
                            .assignedAt(
                                    LocalDateTime.now(
                                            ZoneOffset.UTC
                                    )
                            )
                            .onboardingAssignment(
                                    false
                            )
                            .build();

            assignmentRepository.save(
                    assignment
            );

            notificationRepository.save(
                    Notification
                            .builder()
                            .recipient(
                                    employee
                            )
                            .policy(
                                    policy
                            )
                            .title(
                                    "New policy requires acknowledgement"
                            )
                            .message(
                                    policy.getCode()
                                            + " - "
                                            + policy.getName()
                                            + " must be acknowledged by "
                                            + deadline
                            )
                            .status(
                                    NotificationStatus.PENDING
                            )
                            .build()
            );

            assignedCount++;
        }

        log.info(
                "event=POLICY_ASSIGNMENT_COMPLETED policyId={} versionId={} totalEmployees={} assignedEmployees={} skippedNotApplicable={} skippedExistingAssignment={} deadline={}",
                policy.getId(),
                version.getId(),
                employees.size(),
                assignedCount,
                skippedNotApplicable,
                skippedExistingAssignment,
                deadline
        );

        return assignedCount;
    }


    // =========================================================
    // CHECK POLICY APPLICABILITY
    // =========================================================

    private boolean isApplicable(
            Policy policy,
            User employee
    ) {

        return switch (
                policy.getApplicability()
                ) {

            case ALL ->
                    true;

            case DEPT_BASED ->
                    containsValue(
                            policy.getApplicableDepartments(),
                            employee.getDepartment()
                    );

            case GRADE_BASED ->
                    containsValue(
                            policy.getApplicableGrades(),
                            employee.getGrade()
                    );
        };
    }


    // =========================================================
    // CHECK CONFIGURED VALUE
    // =========================================================

    private boolean containsValue(
            String configuredValues,
            String employeeValue
    ) {

        if (!StringUtils.hasText(
                configuredValues
        )
                ||
                !StringUtils.hasText(
                        employeeValue
                )) {

            return false;
        }

        return Arrays
                .stream(
                        configuredValues.split(",")
                )
                .map(
                        String::trim
                )
                .anyMatch(
                        value ->
                                value.equalsIgnoreCase(
                                        employeeValue.trim()
                                )
                );
    }


    // =========================================================
    // VALIDATE POLICY VERSION ACCESS
    // =========================================================

    private void validateVersionAccess(
            Policy policy
    ) {

        User user =
                currentUser();

        /*
         * Management users may view version history.
         */
        if (user.getRole()
                != Role.EMPLOYEE) {

            return;
        }

        /*
         * Employees must not see policy history
         * until the policy has been published.
         */
        if (!Boolean.TRUE.equals(
                policy.getPublishedOnce()
        )) {

            log.warn(
                    "event=POLICY_VERSION_ACCESS_DENIED policyId={} userId={} reason=NEVER_PUBLISHED",
                    policy.getId(),
                    user.getId()
            );

            throw new ForbiddenException(
                    "You cannot access this policy history"
            );
        }

        /*
         * Employees may only see applicable policies.
         */
        if (!isApplicable(
                policy,
                user
        )) {

            log.warn(
                    "event=POLICY_VERSION_ACCESS_DENIED policyId={} userId={} reason=NOT_APPLICABLE",
                    policy.getId(),
                    user.getId()
            );

            throw new ForbiddenException(
                    "This policy is not applicable to your account"
            );
        }

        log.debug(
                "event=POLICY_VERSION_ACCESS_GRANTED policyId={} userId={} role={}",
                policy.getId(),
                user.getId(),
                user.getRole()
        );
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private User currentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                ||
                !authentication
                        .isAuthenticated()) {

            log.warn(
                    "event=POLICY_PUBLISH_AUTHENTICATION_FAILED reason=NO_AUTHENTICATED_USER"
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
                                    "event=POLICY_PUBLISH_AUTHENTICATION_FAILED reason=USER_NOT_FOUND"
                            );

                            return new UnauthorizedException(
                                    "Authenticated user was not found"
                            );
                        }
                );
    }


    // =========================================================
    // CLEAN OPTIONAL TEXT
    // =========================================================

    private String clean(
            String value
    ) {

        return StringUtils.hasText(
                value
        )
                ? value.trim()
                : null;
    }


    // =========================================================
    // ENTITY -> RESPONSE DTO
    // =========================================================

    private PolicyVersionResponse map(
            PolicyVersion version,
            int assignedEmployees
    ) {

        User publisher =
                version.getPublishedBy();

        Policy policy =
                version.getPolicy();

        return PolicyVersionResponse
                .builder()

                .id(
                        version.getId()
                )

                .policyId(
                        policy.getId()
                )

                .policyCode(
                        policy.getCode()
                )

                .policyName(
                        policy.getName()
                )

                .versionNumber(
                        version.getVersionNumber()
                )

                .content(
                        version.getContentSnapshot()
                )

                .effectiveDate(
                        version.getEffectiveDate()
                )

                .publishedAt(
                        version.getPublishedAt()
                )

                .publishedById(
                        publisher.getId()
                )

                .publishedByName(
                        publisher.getName()
                )

                .publishedByEmail(
                        publisher.getEmail()
                )

                .changeSummary(
                        version.getChangeSummary()
                )

                .currentVersion(
                        version.getCurrentVersion()
                )

                .archived(
                        version.getArchived()
                )

                .assignedEmployees(
                        assignedEmployees
                )

                .build();
    }
}