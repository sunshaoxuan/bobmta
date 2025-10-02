package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public NotificationResult sendEmail(EmailMessage message) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("to", String.join(",", message.getTo()));
        if (!message.getCc().isEmpty()) {
            metadata.put("cc", String.join(",", message.getCc()));
        }
        log.info("Sending email to {} with subject {}", metadata.get("to"), message.getSubject());
        return NotificationResult.success("EMAIL", "email.dispatched", metadata);
    }

    @Override
    public NotificationResult sendInstantMessage(InstantMessage message) {
        String recipients = message.getRecipients().stream().collect(Collectors.joining(","));
        Map<String, String> metadata = Map.of("recipients", recipients);
        log.info("Sending instant message to {}", recipients);
        return NotificationResult.success("IM", "im.dispatched", metadata);
    }

    @Override
    public NotificationResult invokeApiCall(ApiCallRequest request) {
        log.info("Invoking API {} {}", request.getMethod(), request.getEndpoint());
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("endpoint", request.getEndpoint());
        metadata.put("method", request.getMethod());
        if (!request.getHeaders().isEmpty()) {
            metadata.put("headers", request.getHeaders().keySet().stream().sorted().collect(Collectors.joining(",")));
        }
        return NotificationResult.success("API", "api.invoked", metadata);
    }
}
