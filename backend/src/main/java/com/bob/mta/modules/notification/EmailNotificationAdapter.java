package com.bob.mta.modules.notification;

public interface EmailNotificationAdapter {

    NotificationResult send(EmailMessage message);
}
