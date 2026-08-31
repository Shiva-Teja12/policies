package com.example.hrmspolicies2.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PolicyEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PolicyEmailService.class
            );

    private final JavaMailSender mailSender;

    @Value("${notifications.mail-enabled:false}")
    private boolean mailEnabled;

    @Value("${notifications.from-email:noreply@enfec.com}")
    private String fromEmail;

    public PolicyEmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    public EmailDeliveryResult send(
            String recipient,
            String cc,
            String subject,
            String body
    ) {
        if (!StringUtils.hasText(
                recipient
        )) {
            return EmailDeliveryResult.failure(
                    "Recipient email is missing"
            );
        }

        if (!mailEnabled) {
            log.info(
                    """
                    DEVELOPMENT EMAIL
                    To: {}
                    CC: {}
                    Subject: {}
                    Body: {}
                    """,
                    recipient,
                    cc,
                    subject,
                    body
            );

            return EmailDeliveryResult.success();
        }

        try {
            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(recipient);

            if (StringUtils.hasText(cc)) {
                message.setCc(cc);
            }

            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            return EmailDeliveryResult.success();
        } catch (Exception exception) {
            log.error(
                    "Email delivery failed for {}",
                    recipient,
                    exception
            );

            return EmailDeliveryResult.failure(
                    limit(
                            exception.getMessage(),
                            1900
                    )
            );
        }
    }

    private String limit(
            String value,
            int length
    ) {
        if (value == null) {
            return "Unknown email delivery error";
        }

        return value.length() <= length
                ? value
                : value.substring(0, length);
    }
}