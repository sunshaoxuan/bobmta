package com.bob.mta.modules.user.domain;

import java.time.OffsetDateTime;

public class ActivationToken {

    private final String token;
    private final String userId;
    private final OffsetDateTime expiresAt;

    public ActivationToken(String token, String userId, OffsetDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}
