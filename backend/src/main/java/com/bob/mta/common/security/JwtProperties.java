package com.bob.mta.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties binding for JWT related configuration.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;

    private AccessToken accessToken = new AccessToken();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public static class AccessToken {

        private String secret;

        private long expirationMinutes = 120;

        public String getSecret() {
            return secret;
        }

        public void setSecret(final String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(final long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
