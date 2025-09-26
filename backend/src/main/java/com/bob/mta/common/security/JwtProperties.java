package com.bob.mta.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

<<<<<<< HEAD
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer = "bob-mta";
=======
/**
 * Properties binding for JWT related configuration.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;

>>>>>>> origin/main
    private AccessToken accessToken = new AccessToken();

    public String getIssuer() {
        return issuer;
    }

<<<<<<< HEAD
    public void setIssuer(String issuer) {
=======
    public void setIssuer(final String issuer) {
>>>>>>> origin/main
        this.issuer = issuer;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

<<<<<<< HEAD
    public void setAccessToken(AccessToken accessToken) {
=======
    public void setAccessToken(final AccessToken accessToken) {
>>>>>>> origin/main
        this.accessToken = accessToken;
    }

    public static class AccessToken {
<<<<<<< HEAD
        private String secret = "change-me-please";
=======

        private String secret;

>>>>>>> origin/main
        private long expirationMinutes = 120;

        public String getSecret() {
            return secret;
        }

<<<<<<< HEAD
        public void setSecret(String secret) {
=======
        public void setSecret(final String secret) {
>>>>>>> origin/main
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

<<<<<<< HEAD
        public void setExpirationMinutes(long expirationMinutes) {
=======
        public void setExpirationMinutes(final long expirationMinutes) {
>>>>>>> origin/main
            this.expirationMinutes = expirationMinutes;
        }
    }
}
