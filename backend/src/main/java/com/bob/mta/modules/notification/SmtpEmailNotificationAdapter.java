package com.bob.mta.modules.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
@ConditionalOnBean(JavaMailSender.class)
@ConditionalOnProperty(prefix = "notification.email", name = "enabled", havingValue = "true")
public class SmtpEmailNotificationAdapter implements EmailNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailNotificationAdapter.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpEmailNotificationAdapter(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public NotificationResult send(EmailMessage message) {
        NotificationProperties.Email config = properties.getEmail();
        String from = config.getFrom();
        if (!StringUtils.hasText(from)) {
            return NotificationResult.failure("EMAIL", "email.configuration.missing-from",
                    "Email sender address is not configured", Map.of());
        }
        List<String> toRecipients = message.getTo();
        if (CollectionUtils.isEmpty(toRecipients)) {
            return NotificationResult.failure("EMAIL", "email.configuration.missing-recipient",
                    "Email recipient list is empty", Map.of("reason", "NO_RECIPIENT"));
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            if (StringUtils.hasText(config.getReplyTo())) {
                helper.setReplyTo(config.getReplyTo());
            }
            helper.setTo(toRecipients.toArray(String[]::new));
            if (!message.getCc().isEmpty()) {
                helper.setCc(message.getCc().toArray(String[]::new));
            }
            helper.setSubject(message.getSubject());
            helper.setText(message.getContent() == null ? "" : message.getContent(), true);

            mailSender.send(mimeMessage);

            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("from", from);
            metadata.put("to", String.join(",", toRecipients));
            if (!message.getCc().isEmpty()) {
                metadata.put("cc", String.join(",", message.getCc()));
            }
            log.info("Email dispatched to {}", metadata.get("to"));
            return NotificationResult.success("EMAIL", "email.dispatched", metadata);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP", ex);
            return NotificationResult.failure("EMAIL", "email.dispatch.failed", ex.getMessage(),
                    Map.of("reason", "SMTP_ERROR"));
        }
    }
}
