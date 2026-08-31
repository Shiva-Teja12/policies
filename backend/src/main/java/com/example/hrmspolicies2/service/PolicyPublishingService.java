package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.dto.PublishPolicyRequest;
import com.example.hrmspolicies2.dto.response.PolicyVersionResponse;
import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.exception.*;
import com.example.hrmspolicies2.repository.*;
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

    @Transactional
    public PolicyVersionResponse publish(
            Long policyId,
            PublishPolicyRequest request
    ) {
        Policy policy = policyRepository
                .findByIdForUpdate(policyId)
                .orElseThrow(() ->
                        ResourceNotFoundException
                                .forEntity(
                                        "Policy",
                                        policyId
                                )
                );

        if (policy.getStatus()
                != PolicyStatus.APPROVED) {
            throw new BadRequestException(
                    "Only APPROVED policies can be published"
            );
        }

        User publisher = currentUser();

        int nextVersion =
                versionRepository
                        .findTopByPolicy_IdOrderByVersionNumberDesc(
                                policyId
                        )
                        .map(version ->
                                version.getVersionNumber()
                                        + 1
                        )
                        .orElse(1);

        versionRepository
                .findByPolicy_IdAndCurrentVersionTrue(
                        policyId
                )
                .ifPresent(previous -> {
                    previous.setCurrentVersion(false);
                    previous.setArchived(true);
                    versionRepository.save(previous);
                });

        int acknowledgementDays =
                request.getAcknowledgementPeriodDays()
                        == null
                        ? policy.getAcknowledgementPeriodDays()
                        : request.getAcknowledgementPeriodDays();

        policy.setAcknowledgementPeriodDays(
                acknowledgementDays
        );

        PolicyVersion version =
                PolicyVersion.builder()
                        .policy(policy)
                        .versionNumber(nextVersion)
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
                        .publishedBy(publisher)
                        .changeSummary(
                                clean(
                                        request.getChangeSummary()
                                )
                        )
                        .currentVersion(true)
                        .archived(false)
                        .build();

        PolicyVersion savedVersion =
                versionRepository.saveAndFlush(
                        version
                );

        policy.setStatus(
                PolicyStatus.PUBLISHED
        );

        policy.setPublishedOnce(true);

        policy.setEffectiveDate(
                request.getEffectiveDate()
        );

        policyRepository.save(policy);

        int assignedEmployees =
                assignToApplicableEmployees(
                        policy,
                        savedVersion,
                        acknowledgementDays
                );

        return map(
                savedVersion,
                assignedEmployees
        );
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getVersions(
            Long policyId
    ) {
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(() ->
                                ResourceNotFoundException
                                        .forEntity(
                                                "Policy",
                                                policyId
                                        )
                        );

        validateVersionAccess(policy);

        return versionRepository
                .findByPolicy_IdOrderByVersionNumberDesc(
                        policyId
                )
                .stream()
                .map(version ->
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
    }

    @Transactional(readOnly = true)
    public PolicyVersionResponse getVersion(
            Long policyId,
            Integer versionNumber
    ) {
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(() ->
                                ResourceNotFoundException
                                        .forEntity(
                                                "Policy",
                                                policyId
                                        )
                        );

        validateVersionAccess(policy);

        PolicyVersion version =
                versionRepository
                        .findByPolicy_IdAndVersionNumber(
                                policyId,
                                versionNumber
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Policy version was not found"
                                )
                        );

        return map(
                version,
                Math.toIntExact(
                        assignmentRepository
                                .countByPolicyVersion_Id(
                                        version.getId()
                                )
                )
        );
    }

    private int assignToApplicableEmployees(
            Policy policy,
            PolicyVersion version,
            int acknowledgementDays
    ) {
        List<User> employees =
                userRepository.findByRole(
                        Role.EMPLOYEE
                );

        LocalDate deadline =
                LocalDate.now(ZoneOffset.UTC)
                        .plusDays(
                                acknowledgementDays
                        );

        int assignedCount = 0;

        for (User employee : employees) {
            if (!isApplicable(
                    policy,
                    employee
            )) {
                continue;
            }

            if (assignmentRepository
                    .existsByEmployee_IdAndPolicyVersion_Id(
                            employee.getId(),
                            version.getId()
                    )) {
                continue;
            }

            PolicyAssignment assignment =
                    PolicyAssignment.builder()
                            .employee(employee)
                            .policy(policy)
                            .policyVersion(version)
                            .deadline(deadline)
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
                    Notification.builder()
                            .recipient(employee)
                            .policy(policy)
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

        return assignedCount;
    }

    private boolean isApplicable(
            Policy policy,
            User employee
    ) {
        return switch (
                policy.getApplicability()
                ) {
            case ALL -> true;

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

    private boolean containsValue(
            String configuredValues,
            String employeeValue
    ) {
        if (!StringUtils.hasText(
                configuredValues
        ) || !StringUtils.hasText(
                employeeValue
        )) {
            return false;
        }

        return Arrays.stream(
                        configuredValues.split(",")
                )
                .map(String::trim)
                .anyMatch(value ->
                        value.equalsIgnoreCase(
                                employeeValue.trim()
                        )
                );
    }

    private void validateVersionAccess(
            Policy policy
    ) {
        User user = currentUser();

        if (user.getRole()
                != Role.EMPLOYEE) {
            return;
        }

        if (!Boolean.TRUE.equals(
                policy.getPublishedOnce()
        )) {
            throw new ForbiddenException(
                    "You cannot access this policy history"
            );
        }

        if (!isApplicable(policy, user)) {
            throw new ForbiddenException(
                    "This policy is not applicable to your account"
            );
        }
    }

    private User currentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new UnauthorizedException(
                    "Authentication is required"
            );
        }

        return userRepository
                .findByEmailIgnoreCase(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Authenticated user was not found"
                        )
                );
    }

    private String clean(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }

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
                .id(version.getId())
                .policyId(policy.getId())
                .policyCode(policy.getCode())
                .policyName(policy.getName())
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