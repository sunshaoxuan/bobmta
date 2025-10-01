package com.bob.mta.modules.notification;

import java.util.Map;

public class NotificationResult {

    private final boolean success;
    private final String channel;
    private final String message;
    private final String error;
    private final Map<String, String> metadata;

    private NotificationResult(boolean success, String channel, String message, String error,
                               Map<String, String> metadata) {
        this.success = success;
        this.channel = channel;
        this.message = message;
        this.error = error;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static NotificationResult success(String channel, String message, Map<String, String> metadata) {
        return new NotificationResult(true, channel, message, null, metadata);
    }

    public static NotificationResult failure(String channel, String message, String error, Map<String, String> metadata) {
        return new NotificationResult(false, channel, message, error, metadata);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
