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
    private boolean emailAlwaysFail;
    private int emailFailuresRemaining;
    private boolean imAlwaysFail;
    private int imFailuresRemaining;
    private String emailError = "email.failed";
    private String imError = "im.failed";

    @Override
    public NotificationResult sendEmail(EmailMessage message) {
        emails.add(message);
        boolean shouldFail = emailAlwaysFail || emailFailuresRemaining > 0;
        if (emailFailuresRemaining > 0) {
            emailFailuresRemaining--;
        }
        if (shouldFail) {
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
        boolean shouldFail = imAlwaysFail || imFailuresRemaining > 0;
        if (imFailuresRemaining > 0) {
            imFailuresRemaining--;
        }
        if (shouldFail) {
            return NotificationResult.failure("IM", "im.failed", imError, Map.of());
        }
        return NotificationResult.success("IM", "im.sent", Map.of("recipients", String.join(",", message.getRecipients())));
    }

    void failEmail(String error) {
        this.emailAlwaysFail = true;
        this.emailError = error;
    }

    void failInstantMessage(String error) {
        this.imAlwaysFail = true;
        this.imError = error;
    }

    void failEmailTimes(int times, String error) {
        this.emailAlwaysFail = false;
        this.emailFailuresRemaining = Math.max(times, 0);
        this.emailError = error;
    }

    void failInstantMessageTimes(int times, String error) {
        this.imAlwaysFail = false;
        this.imFailuresRemaining = Math.max(times, 0);
        this.imError = error;
    }

    List<EmailMessage> getEmails() {
        return emails;
    }

    List<InstantMessage> getInstantMessages() {
        return instantMessages;
    }
}
