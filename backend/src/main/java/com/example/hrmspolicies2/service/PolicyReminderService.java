package com.example.hrmspolicies2.service;

import com.example.hrmspolicies2.entity.*;
import com.example.hrmspolicies2.enums.*;
import com.example.hrmspolicies2.notification.EmailDeliveryResult;
import com.example.hrmspolicies2.notification.PolicyEmailService;
import com.example.hrmspolicies2.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class PolicyReminderService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyReminderService.class
            );

    private final PolicyAssignmentRepository assignmentRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PolicyEmailService emailService;


    public PolicyReminderService(
            PolicyAssignmentRepository assignmentRepository,
            PolicyAcknowledgementRepository acknowledgementRepository,
            ReminderLogRepository reminderLogRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            PolicyEmailService emailService
    ) {
        this.assignmentRepository =
                assignmentRepository;

        this.acknowledgementRepository =
                acknowledgementRepository;

        this.reminderLogRepository =
                reminderLogRepository;

        this.notificationRepository =
                notificationRepository;

        this.userRepository =
                userRepository;

        this.emailService =
                emailService;
    }


    // =========================================================
    // PROCESS DAILY REMINDERS
    // =========================================================

    @Transactional
    public void processDailyReminders() {

        LocalDate today =
                LocalDate.now(
                        ZoneOffset.UTC
                );

        log.info(
                "event=POLICY_REMINDER_JOB_STARTED date={}",
                today
        );

        List<PolicyAssignment> overdueAssignments =
                assignmentRepository
                        .findByStatusInAndDeadlineBefore(
                                EnumSet.of(
                                        AssignmentStatus.PENDING,
                                        AssignmentStatus.OVERDUE
                                ),
                                today
                        );

        log.info(
                "event=POLICY_REMINDER_OVERDUE_ASSIGNMENTS_FOUND date={} count={}",
                today,
                overdueAssignments.size()
        );

        int processedCount = 0;

        for (PolicyAssignment assignment :
                overdueAssignments) {

            processAssignment(
                    assignment,
                    today
            );

            processedCount++;
        }

        log.info(
                "event=POLICY_REMINDER_JOB_COMPLETED date={} processedAssignments={}",
                today,
                processedCount
        );
    }


    // =========================================================
    // PROCESS SINGLE ASSIGNMENT
    // =========================================================

    private void processAssignment(
            PolicyAssignment assignment,
            LocalDate today
    ) {

        PolicyVersion version =
                assignment.getPolicyVersion();

        if (!Boolean.TRUE.equals(
                version.getCurrentVersion()
        )) {

            log.debug(
                    "event=POLICY_REMINDER_SKIPPED assignmentId={} policyId={} reason=NOT_CURRENT_VERSION",
                    assignment.getId(),
                    assignment.getPolicy()
                            .getId()
            );

            return;
        }


        if (acknowledgementRepository
                .existsByEmployee_IdAndPolicyVersion_Id(
                        assignment.getEmployee()
                                .getId(),
                        version.getId()
                )) {

            assignment.setStatus(
                    AssignmentStatus.ACKNOWLEDGED
            );

            assignmentRepository.save(
                    assignment
            );

            log.info(
                    "event=POLICY_REMINDER_ASSIGNMENT_ACKNOWLEDGED assignmentId={} policyId={} employeeId={}",
                    assignment.getId(),
                    assignment.getPolicy()
                            .getId(),
                    assignment.getEmployee()
                            .getId()
            );

            return;
        }


        long daysOverdue =
                ChronoUnit.DAYS.between(
                        assignment.getDeadline(),
                        today
                );


        Optional<ReminderStage> stage =
                determineStage(
                        daysOverdue
                );


        if (stage.isEmpty()) {

            log.debug(
                    "event=POLICY_REMINDER_SKIPPED assignmentId={} policyId={} employeeId={} reason=NO_REMINDER_STAGE daysOverdue={}",
                    assignment.getId(),
                    assignment.getPolicy()
                            .getId(),
                    assignment.getEmployee()
                            .getId(),
                    daysOverdue
            );

            return;
        }


        ReminderStage reminderStage =
                stage.get();


        if (reminderLogRepository
                .existsByAssignment_IdAndReminderStage(
                        assignment.getId(),
                        reminderStage
                )) {

            log.debug(
                    "event=POLICY_REMINDER_SKIPPED assignmentId={} policyId={} employeeId={} stage={} reason=ALREADY_SENT",
                    assignment.getId(),
                    assignment.getPolicy()
                            .getId(),
                    assignment.getEmployee()
                            .getId(),
                    reminderStage
            );

            return;
        }


        assignment.setStatus(
                AssignmentStatus.OVERDUE
        );

        assignmentRepository.save(
                assignment
        );


        log.info(
                "event=POLICY_REMINDER_DUE assignmentId={} policyId={} employeeId={} stage={} daysOverdue={}",
                assignment.getId(),
                assignment.getPolicy()
                        .getId(),
                assignment.getEmployee()
                        .getId(),
                reminderStage,
                daysOverdue
        );


        deliverReminder(
                assignment,
                reminderStage,
                daysOverdue
        );
    }


    // =========================================================
    // DETERMINE REMINDER STAGE
    // =========================================================

    private Optional<ReminderStage>
    determineStage(
            long daysOverdue
    ) {

        if (daysOverdue >= 14) {

            return Optional.of(
                    ReminderStage
                            .DAY_14_HR_HEAD_ESCALATION
            );
        }

        if (daysOverdue >= 10) {

            return Optional.of(
                    ReminderStage
                            .DAY_10_MANAGER_CC
            );
        }

        if (daysOverdue >= 7) {

            return Optional.of(
                    ReminderStage
                            .DAY_7_SECOND_REMINDER
            );
        }

        if (daysOverdue >= 3) {

            return Optional.of(
                    ReminderStage
                            .DAY_3_FIRST_REMINDER
            );
        }

        return Optional.empty();
    }


    // =========================================================
    // DELIVER REMINDER
    // =========================================================

    private void deliverReminder(
            PolicyAssignment assignment,
            ReminderStage stage,
            long daysOverdue
    ) {

        User employee =
                assignment.getEmployee();

        Policy policy =
                assignment.getPolicy();

        String recipient =
                employee.getEmail();

        String cc = null;


        if (stage
                == ReminderStage
                .DAY_10_MANAGER_CC
                && StringUtils.hasText(
                employee.getManagerEmail()
        )) {

            cc =
                    employee.getManagerEmail();
        }


        if (stage
                == ReminderStage
                .DAY_14_HR_HEAD_ESCALATION) {

            List<User> hrHeads =
                    userRepository.findByRole(
                            Role.HR_HEAD
                    );

            if (!hrHeads.isEmpty()) {

                recipient =
                        hrHeads.get(0)
                                .getEmail();

                cc =
                        employee.getEmail();

            } else {

                log.warn(
                        "event=POLICY_REMINDER_ESCALATION_FALLBACK assignmentId={} policyId={} employeeId={} reason=HR_HEAD_NOT_FOUND",
                        assignment.getId(),
                        policy.getId(),
                        employee.getId()
                );
            }
        }


        String subject =
                subjectFor(
                        stage,
                        policy
                );


        String body =
                bodyFor(
                        stage,
                        employee,
                        policy,
                        assignment,
                        daysOverdue
                );


        log.info(
                "event=POLICY_REMINDER_SEND_REQUEST assignmentId={} policyId={} employeeId={} stage={} daysOverdue={} hasCc={}",
                assignment.getId(),
                policy.getId(),
                employee.getId(),
                stage,
                daysOverdue,
                StringUtils.hasText(
                        cc
                )
        );


        EmailDeliveryResult deliveryResult =
                emailService.send(
                        recipient,
                        cc,
                        subject,
                        body
                );


        NotificationStatus deliveryStatus =
                deliveryResult.successful()
                        ? NotificationStatus.SENT
                        : NotificationStatus.FAILED;


        reminderLogRepository.save(
                ReminderLog
                        .builder()
                        .assignment(
                                assignment
                        )
                        .employee(
                                employee
                        )
                        .policy(
                                policy
                        )
                        .reminderStage(
                                stage
                        )
                        .deliveryStatus(
                                deliveryStatus
                        )
                        .recipientEmail(
                                recipient
                        )
                        .ccEmail(
                                cc
                        )
                        .sentAt(
                                LocalDateTime.now(
                                        ZoneOffset.UTC
                                )
                        )
                        .failureReason(
                                deliveryResult
                                        .failureReason()
                        )
                        .build()
        );


        notificationRepository.save(
                Notification
                        .builder()
                        .recipient(
                                stage
                                        == ReminderStage
                                        .DAY_14_HR_HEAD_ESCALATION
                                        ? findHrHeadOrEmployee(
                                        employee
                                )
                                        : employee
                        )
                        .policy(
                                policy
                        )
                        .title(
                                subject
                        )
                        .message(
                                body
                        )
                        .status(
                                deliveryStatus
                        )
                        .sentAt(
                                deliveryResult
                                        .successful()
                                        ? LocalDateTime.now(
                                        ZoneOffset.UTC
                                )
                                        : null
                        )
                        .build()
        );


        if (deliveryResult.successful()) {

            log.info(
                    "event=POLICY_REMINDER_SENT assignmentId={} policyId={} employeeId={} stage={} deliveryStatus={}",
                    assignment.getId(),
                    policy.getId(),
                    employee.getId(),
                    stage,
                    deliveryStatus
            );

        } else {

            log.warn(
                    "event=POLICY_REMINDER_DELIVERY_FAILED assignmentId={} policyId={} employeeId={} stage={} deliveryStatus={}",
                    assignment.getId(),
                    policy.getId(),
                    employee.getId(),
                    stage,
                    deliveryStatus
            );
        }
    }


    // =========================================================
    // FIND HR HEAD OR FALL BACK TO EMPLOYEE
    // =========================================================

    private User findHrHeadOrEmployee(
            User employee
    ) {

        return userRepository
                .findByRole(
                        Role.HR_HEAD
                )
                .stream()
                .findFirst()
                .orElse(
                        employee
                );
    }


    // =========================================================
    // EMAIL SUBJECT
    // =========================================================

    private String subjectFor(
            ReminderStage stage,
            Policy policy
    ) {

        return switch (stage) {

            case DAY_3_FIRST_REMINDER ->
                    "Policy acknowledgement reminder: "
                            + policy.getCode();

            case DAY_7_SECOND_REMINDER ->
                    "Second policy acknowledgement reminder: "
                            + policy.getCode();

            case DAY_10_MANAGER_CC ->
                    "Overdue policy acknowledgement: "
                            + policy.getCode();

            case DAY_14_HR_HEAD_ESCALATION ->
                    "HR escalation: overdue policy acknowledgement";
        };
    }


    // =========================================================
    // EMAIL BODY
    // =========================================================

    private String bodyFor(
            ReminderStage stage,
            User employee,
            Policy policy,
            PolicyAssignment assignment,
            long daysOverdue
    ) {

        String base =
                "Employee: "
                        + employee.getName()
                        + " ("
                        + employee.getEmail()
                        + ")\n"
                        + "Policy: "
                        + policy.getCode()
                        + " - "
                        + policy.getName()
                        + "\nDeadline: "
                        + assignment.getDeadline()
                        + "\nDays overdue: "
                        + daysOverdue;


        return switch (stage) {

            case DAY_3_FIRST_REMINDER ->
                    "This is the first reminder to acknowledge the policy.\n\n"
                            + base;

            case DAY_7_SECOND_REMINDER ->
                    "This is the second reminder to acknowledge the policy immediately.\n\n"
                            + base;

            case DAY_10_MANAGER_CC ->
                    "The acknowledgement is overdue. The employee's manager has been copied.\n\n"
                            + base;

            case DAY_14_HR_HEAD_ESCALATION ->
                    "This acknowledgement has been escalated to the HR Head.\n\n"
                            + base;
        };
    }
}