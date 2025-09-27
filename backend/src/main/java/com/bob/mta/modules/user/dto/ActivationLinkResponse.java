package com.bob.mta.modules.user.dto;

import java.time.OffsetDateTime;

public class ActivationLinkResponse {

    private final String token;
    private final OffsetDateTime expiresAt;

    public ActivationLinkResponse(String token, OffsetDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
