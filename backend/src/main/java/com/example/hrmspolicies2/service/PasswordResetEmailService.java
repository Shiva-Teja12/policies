package com.example.hrmspolicies2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    // =========================================================
    // STRUCTURED LOGGING
    // =========================================================

    private static final Logger log =
            LoggerFactory.getLogger(
                    PasswordResetEmailService.class
            );

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;


    public PasswordResetEmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender =
                mailSender;
    }


    // =========================================================
    // SEND PASSWORD RESET EMAIL
    // =========================================================

    public void sendResetEmail(
            String email,
            String name,
            String token
    ) {

        log.info(
                "event=PASSWORD_RESET_EMAIL_SEND_REQUEST recipient={}",
                email
        );

        /*
         * IMPORTANT:
         * The reset token is used only to build the link.
         * Never log the token or reset link.
         */
        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + token;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                fromEmail
        );

        message.setTo(
                email
        );

        message.setSubject(
                "HRMS Password Reset"
        );

        message.setText(
                "Hello " + name + ",\n\n"
                        + "We received a request to reset your "
                        + "HRMS password.\n\n"
                        + "Click the link below to reset your password:\n\n"
                        + resetLink
                        + "\n\n"
                        + "This link expires in 15 minutes.\n\n"
                        + "If you did not request this password reset, "
                        + "you can ignore this email.\n\n"
                        + "HRMS Team"
        );

        try {

            mailSender.send(
                    message
            );

            log.info(
                    "event=PASSWORD_RESET_EMAIL_SENT recipient={}",
                    email
            );

        } catch (MailException exception) {

            log.error(
                    "event=PASSWORD_RESET_EMAIL_FAILED recipient={} exceptionType={}",
                    email,
                    exception.getClass()
                            .getSimpleName(),
                    exception
            );

            throw exception;
        }
    }


    // =========================================================
    // SEND PASSWORD CHANGED CONFIRMATION
    // =========================================================

    public void sendPasswordChangedEmail(
            String email,
            String name
    ) {

        log.info(
                "event=PASSWORD_CHANGED_EMAIL_SEND_REQUEST recipient={}",
                email
        );

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                fromEmail
        );

        message.setTo(
                email
        );

        message.setSubject(
                "HRMS Password Changed Successfully"
        );

        message.setText(
                "Hello " + name + ",\n\n"
                        + "Your HRMS password was changed successfully.\n\n"
                        + "If you did not make this change, "
                        + "please contact HR immediately.\n\n"
                        + "HRMS Team"
        );

        try {

            mailSender.send(
                    message
            );

            log.info(
                    "event=PASSWORD_CHANGED_EMAIL_SENT recipient={}",
                    email
            );

        } catch (MailException exception) {

            log.error(
                    "event=PASSWORD_CHANGED_EMAIL_FAILED recipient={} exceptionType={}",
                    email,
                    exception.getClass()
                            .getSimpleName(),
                    exception
            );

            throw exception;
        }
    }
}