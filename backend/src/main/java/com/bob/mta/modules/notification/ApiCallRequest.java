package com.bob.mta.modules.notification;

import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates an outbound API invocation that can be executed by the
 * {@link ApiNotificationAdapter}. The request keeps the HTTP method, target
 * endpoint, optional payload and headers so concrete adapter implementations
 * can decide how to execute the call (REST, webhook, workflow engine, etc.).
 */
public class ApiCallRequest {

    private final String endpoint;
    private final String method;
    private final String body;
    private final Map<String, String> headers;

    public ApiCallRequest(String endpoint, String method, String body, Map<String, String> headers) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.method = Objects.requireNonNull(method, "method");
        this.body = body;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}

