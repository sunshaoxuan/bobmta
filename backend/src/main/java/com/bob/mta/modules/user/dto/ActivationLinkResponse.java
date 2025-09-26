package com.bob.mta.modules.user.dto;

<<<<<<< HEAD
import java.time.OffsetDateTime;

public class ActivationLinkResponse {

    private final String token;
    private final OffsetDateTime expiresAt;

    public ActivationLinkResponse(String token, OffsetDateTime expiresAt) {
=======
import com.bob.mta.modules.user.service.model.ActivationLink;
import java.time.Instant;

/**
 * Response envelope returning an activation link token and expiry.
 */
public class ActivationLinkResponse {

    private final String token;

    private final Instant expiresAt;

    public ActivationLinkResponse(final String token, final Instant expiresAt) {
>>>>>>> origin/main
        this.token = token;
        this.expiresAt = expiresAt;
    }

<<<<<<< HEAD
=======
    public static ActivationLinkResponse from(final ActivationLink link) {
        return new ActivationLinkResponse(link.token(), link.expiresAt());
    }

>>>>>>> origin/main
    public String getToken() {
        return token;
    }

<<<<<<< HEAD
    public OffsetDateTime getExpiresAt() {
=======
    public Instant getExpiresAt() {
>>>>>>> origin/main
        return expiresAt;
    }
}
