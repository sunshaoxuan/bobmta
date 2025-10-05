package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "notification.instant-message", name = "enabled", havingValue = "true")
public class WebhookInstantMessageNotificationAdapter implements InstantMessageNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebhookInstantMessageNotificationAdapter.class);

    private final RestOperations restOperations;
    private final NotificationProperties properties;

    public WebhookInstantMessageNotificationAdapter(RestTemplateBuilder builder, NotificationProperties properties) {
        NotificationProperties.InstantMessage config = properties.getInstantMessage();
        Duration connectTimeout = config.getConnectTimeout() == null ? Duration.ofSeconds(5) : config.getConnectTimeout();
        Duration readTimeout = config.getReadTimeout() == null ? Duration.ofSeconds(10) : config.getReadTimeout();
        this.restOperations = builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
        this.properties = properties;
    }

    WebhookInstantMessageNotificationAdapter(RestOperations restOperations, NotificationProperties properties) {
        this.restOperations = restOperations;
        this.properties = properties;
    }

    @Override
    public NotificationResult send(InstantMessage message) {
        NotificationProperties.InstantMessage config = properties.getInstantMessage();
        String webhookUrl = config.getWebhookUrl();
        if (!StringUtils.hasText(webhookUrl)) {
            return NotificationResult.failure("IM", "im.configuration.missing-webhook",
                    "Instant message webhook URL is not configured", Map.of());
        }
        try {
            Map<String, Object> payload = buildPayload(message);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restOperations.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("webhook", webhookUrl);
            metadata.put("recipients", String.join(",", message.getRecipients()));
            log.info("Instant message dispatched to {}", metadata.get("recipients"));
            return NotificationResult.success("IM", "im.dispatched", metadata);
        } catch (RestClientException ex) {
            log.error("Failed to send instant message", ex);
            return NotificationResult.failure("IM", "im.dispatch.failed", ex.getMessage(),
                    Map.of("reason", "WEBHOOK_ERROR"));
        }
    }

    private Map<String, Object> buildPayload(InstantMessage message) {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", message.getContent());
        if (!CollectionUtils.isEmpty(message.getRecipients())) {
            text.put("mentioned_list", message.getRecipients());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", text);
        return payload;
    }
}
