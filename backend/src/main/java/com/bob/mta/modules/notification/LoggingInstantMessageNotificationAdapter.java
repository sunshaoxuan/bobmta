package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LoggingInstantMessageNotificationAdapter implements InstantMessageNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingInstantMessageNotificationAdapter.class);

    @Override
    public NotificationResult send(InstantMessage message) {
        String recipients = message.getRecipients().stream().collect(Collectors.joining(","));
        Map<String, String> metadata = Map.of("recipients", recipients);
        log.info("Sending instant message to {}", recipients);
        return NotificationResult.success("IM", "im.dispatched", metadata);
    }
}
