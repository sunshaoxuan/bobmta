package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.modules.notification.EmailMessage;
import com.bob.mta.modules.notification.InstantMessage;
import com.bob.mta.modules.notification.NotificationGateway;
import com.bob.mta.modules.notification.NotificationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingNotificationGateway implements NotificationGateway {

    private final List<EmailMessage> emails = new ArrayList<>();
    private final List<InstantMessage> instantMessages = new ArrayList<>();
    private boolean emailFailure;
    private boolean imFailure;
    private String emailError = "email.failed";
    private String imError = "im.failed";

    @Override
    public NotificationResult sendEmail(EmailMessage message) {
        emails.add(message);
        if (emailFailure) {
            return NotificationResult.failure("EMAIL", "email.failed", emailError, Map.of());
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("to", String.join(",", message.getTo()));
        if (!message.getCc().isEmpty()) {
            metadata.put("cc", String.join(",", message.getCc()));
        }
        return NotificationResult.success("EMAIL", "email.sent", metadata);
    }

    @Override
    public NotificationResult sendInstantMessage(InstantMessage message) {
        instantMessages.add(message);
        if (imFailure) {
            return NotificationResult.failure("IM", "im.failed", imError, Map.of());
        }
        return NotificationResult.success("IM", "im.sent", Map.of("recipients", String.join(",", message.getRecipients())));
    }

    void failEmail(String error) {
        this.emailFailure = true;
        this.emailError = error;
    }

    void failInstantMessage(String error) {
        this.imFailure = true;
        this.imError = error;
    }

    List<EmailMessage> getEmails() {
        return emails;
    }

    List<InstantMessage> getInstantMessages() {
        return instantMessages;
    }
}
