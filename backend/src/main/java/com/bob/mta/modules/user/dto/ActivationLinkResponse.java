package com.bob.mta.modules.user.dto;

import com.bob.mta.modules.user.service.model.ActivationLink;

import java.time.Instant;

/**
 * Response envelope returning an activation link token and expiry.
 */
public class ActivationLinkResponse {

    private final String token;

    private final Instant expiresAt;

    public ActivationLinkResponse(final String token, final Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public static ActivationLinkResponse from(final ActivationLink activationLink) {
        return new ActivationLinkResponse(activationLink.token(), activationLink.expiresAt());
    }
}