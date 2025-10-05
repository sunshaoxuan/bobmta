package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LoggingApiNotificationAdapter implements ApiNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingApiNotificationAdapter.class);

    @Override
    public NotificationResult invoke(ApiCallRequest request) {
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
