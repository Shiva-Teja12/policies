package com.example.hrmspolicies2.scheduler;

import com.example.hrmspolicies2.service.PolicyReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PolicyReminderScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyReminderScheduler.class
            );

    private final PolicyReminderService reminderService;

    public PolicyReminderScheduler(
            PolicyReminderService reminderService
    ) {
        this.reminderService =
                reminderService;
    }

    @Scheduled(
            cron = "0 0 9 * * *",
            zone = "Asia/Kolkata"
    )
    public void runDailyReminders() {
        log.info(
                "Starting daily policy reminder job"
        );

        reminderService
                .processDailyReminders();

        log.info(
                "Daily policy reminder job completed"
        );
    }
}