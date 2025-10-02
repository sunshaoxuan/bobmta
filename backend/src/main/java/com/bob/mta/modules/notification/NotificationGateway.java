package com.bob.mta.modules.notification;

public interface NotificationGateway {

    NotificationResult sendEmail(EmailMessage message);

    NotificationResult sendInstantMessage(InstantMessage message);

    NotificationResult invokeApiCall(ApiCallRequest request);
}
