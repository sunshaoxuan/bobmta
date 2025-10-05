package com.bob.mta.modules.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "notification.api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpApiNotificationAdapter implements ApiNotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpApiNotificationAdapter.class);

    private final RestOperations restOperations;

    public HttpApiNotificationAdapter(RestTemplateBuilder builder, NotificationProperties properties) {
        NotificationProperties.Api config = properties.getApi();
        Duration connectTimeout = config.getConnectTimeout() == null ? Duration.ofSeconds(5) : config.getConnectTimeout();
        Duration readTimeout = config.getReadTimeout() == null ? Duration.ofSeconds(15) : config.getReadTimeout();
        this.restOperations = builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    HttpApiNotificationAdapter(RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    @Override
    public NotificationResult invoke(ApiCallRequest request) {
        HttpMethod method = resolveMethod(request.getMethod());
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(headers::add);
            }
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            HttpEntity<String> entity = new HttpEntity<>(request.getBody(), headers);
            ResponseEntity<String> response = restOperations.exchange(request.getEndpoint(), method, entity, String.class);
            int status = response.getStatusCode().value();
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("endpoint", request.getEndpoint());
            metadata.put("status", String.valueOf(status));
            if (StringUtils.hasText(response.getBody())) {
                metadata.put("body", truncate(response.getBody()));
            }
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("API call to {} succeeded with status {}", request.getEndpoint(), status);
                return NotificationResult.success("API", "api.invoked", metadata);
            }
            log.warn("API call to {} failed with status {}", request.getEndpoint(), status);
            return NotificationResult.failure("API", "api.failed", "HTTP status %d".formatted(status), metadata);
        } catch (RestClientException ex) {
            log.error("Failed to invoke API", ex);
            return NotificationResult.failure("API", "api.failed", ex.getMessage(),
                    Map.of("reason", "HTTP_ERROR"));
        }
    }

    private HttpMethod resolveMethod(String method) {
        if (!StringUtils.hasText(method)) {
            return HttpMethod.POST;
        }
        try {
            return HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return HttpMethod.POST;
        }
    }

    private String truncate(String body) {
        if (body.length() <= 512) {
            return body;
        }
        return body.substring(0, 512);
    }
}
