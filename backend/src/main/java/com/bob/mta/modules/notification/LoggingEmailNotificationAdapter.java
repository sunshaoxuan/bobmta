package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnMissingBean(EmailNotificationAdapter.class)
public class LoggingEmailNotificationAdapter implements EmailNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationAdapter.class);

    @Override
    public NotificationResult send(EmailMessage message) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("to", String.join(",", message.getTo()));
        if (!message.getCc().isEmpty()) {
            metadata.put("cc", String.join(",", message.getCc()));
        }
        log.info("Sending email to {} with subject {}", metadata.get("to"), message.getSubject());
        return NotificationResult.success("EMAIL", "email.dispatched", metadata);
    }
}
