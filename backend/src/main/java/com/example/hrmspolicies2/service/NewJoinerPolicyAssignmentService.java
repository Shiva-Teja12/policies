package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    NewJoinerPolicyAssignmentService.class
            );

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


    // =========================================================
    // ASSIGN MANDATORY POLICIES TO NEW JOINER
    // =========================================================

    @Transactional
    public int assignMandatoryPolicies(
            User employee
    ) {

        log.info(
                "event=NEW_JOINER_POLICY_ASSIGNMENT_REQUEST userId={} role={}",
                employee.getId(),
                employee.getRole()
        );


        if (employee.getRole()
                != Role.EMPLOYEE) {

            log.debug(
                    "event=NEW_JOINER_POLICY_ASSIGNMENT_SKIPPED userId={} reason=NOT_EMPLOYEE role={}",
                    employee.getId(),
                    employee.getRole()
            );

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


        log.info(
                "event=NEW_JOINER_MANDATORY_POLICIES_FOUND userId={} policyCount={}",
                employee.getId(),
                mandatoryPolicies.size()
        );


        int assignedCount =
                0;

        int skippedNotApplicable =
                0;

        int skippedNoCurrentVersion =
                0;

        int skippedExistingAssignment =
                0;


        for (Policy policy :
                mandatoryPolicies) {


            if (!isApplicable(
                    policy,
                    employee
            )) {

                skippedNotApplicable++;

                log.debug(
                        "event=NEW_JOINER_POLICY_SKIPPED userId={} policyId={} reason=NOT_APPLICABLE",
                        employee.getId(),
                        policy.getId()
                );

                continue;
            }


            PolicyVersion currentVersion =
                    versionRepository
                            .findByPolicy_IdAndCurrentVersionTrue(
                                    policy.getId()
                            )
                            .orElse(
                                    null
                            );


            if (currentVersion == null) {

                skippedNoCurrentVersion++;

                log.warn(
                        "event=NEW_JOINER_POLICY_SKIPPED userId={} policyId={} reason=CURRENT_VERSION_NOT_FOUND",
                        employee.getId(),
                        policy.getId()
                );

                continue;
            }


            if (assignmentRepository
                    .existsByEmployee_IdAndPolicyVersion_Id(
                            employee.getId(),
                            currentVersion.getId()
                    )) {

                skippedExistingAssignment++;

                log.debug(
                        "event=NEW_JOINER_POLICY_SKIPPED userId={} policyId={} versionId={} reason=ALREADY_ASSIGNED",
                        employee.getId(),
                        policy.getId(),
                        currentVersion.getId()
                );

                continue;
            }


            LocalDate deadline =
                    joiningDate.plusDays(
                            policy.getOnboardingPeriodDays()
                    );


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
                                    currentVersion
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
                                    true
                            )
                            .build();


            PolicyAssignment savedAssignment =
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


            log.info(
                    "event=NEW_JOINER_POLICY_ASSIGNED userId={} policyId={} versionId={} assignmentId={} deadline={}",
                    employee.getId(),
                    policy.getId(),
                    currentVersion.getId(),
                    savedAssignment.getId(),
                    deadline
            );
        }


        log.info(
                "event=NEW_JOINER_POLICY_ASSIGNMENT_COMPLETED userId={} totalPolicies={} assignedCount={} skippedNotApplicable={} skippedNoCurrentVersion={} skippedExistingAssignment={}",
                employee.getId(),
                mandatoryPolicies.size(),
                assignedCount,
                skippedNotApplicable,
                skippedNoCurrentVersion,
                skippedExistingAssignment
        );


        return assignedCount;
    }


    // =========================================================
    // POLICY APPLICABILITY
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
    // VALUE MATCHING
    // =========================================================

    private boolean containsValue(
            String configuredValues,
            String employeeValue
    ) {

        if (!StringUtils.hasText(
                configuredValues
        )
                || !StringUtils.hasText(
                employeeValue
        )) {

            return false;
        }


        return Arrays.stream(
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
}