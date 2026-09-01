package com.example.hrmspolicies2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public PasswordResetEmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    public void sendResetEmail(
            String email,
            String name,
            String token
    ) {

        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + token;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);

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

        mailSender.send(message);
    }

    public void sendPasswordChangedEmail(
            String email,
            String name
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);

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

        mailSender.send(message);
    }
}