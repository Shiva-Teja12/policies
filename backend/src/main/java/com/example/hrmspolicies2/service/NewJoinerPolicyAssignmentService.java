package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
public class NewJoinerPolicyAssignmentService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final PolicyAssignmentRepository assignmentRepository;
    private final NotificationRepository notificationRepository;

    public NewJoinerPolicyAssignmentService(
            PolicyRepository policyRepository,
            PolicyVersionRepository versionRepository,
            PolicyAssignmentRepository assignmentRepository,
            NotificationRepository notificationRepository
    ) {
        this.policyRepository =
                policyRepository;

        this.versionRepository =
                versionRepository;

        this.assignmentRepository =
                assignmentRepository;

        this.notificationRepository =
                notificationRepository;
    }

    @Transactional
    public int assignMandatoryPolicies(
            User employee
    ) {
        if (employee.getRole()
                != Role.EMPLOYEE) {
            return 0;
        }

        LocalDate joiningDate =
                employee.getDateOfJoining()
                        == null
                        ? LocalDate.now(
                        ZoneOffset.UTC
                )
                        : employee
                        .getDateOfJoining();

        List<Policy> mandatoryPolicies =
                policyRepository
                        .findByStatusAndMandatoryTrue(
                                PolicyStatus.PUBLISHED
                        );

        int assignedCount = 0;

        for (Policy policy :
                mandatoryPolicies) {
            if (!isApplicable(
                    policy,
                    employee
            )) {
                continue;
            }

            PolicyVersion currentVersion =
                    versionRepository
                            .findByPolicy_IdAndCurrentVersionTrue(
                                    policy.getId()
                            )
                            .orElse(null);

            if (currentVersion == null) {
                continue;
            }

            if (assignmentRepository
                    .existsByEmployee_IdAndPolicyVersion_Id(
                            employee.getId(),
                            currentVersion.getId()
                    )) {
                continue;
            }

            LocalDate deadline =
                    joiningDate.plusDays(
                            policy.getOnboardingPeriodDays()
                    );

            PolicyAssignment assignment =
                    PolicyAssignment.builder()
                            .employee(employee)
                            .policy(policy)
                            .policyVersion(
                                    currentVersion
                            )
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
                                    true
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
                                    "Mandatory new-joiner policy"
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
}