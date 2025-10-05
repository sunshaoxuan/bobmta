package com.bob.mta.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * JWT token provider backed by {@link io.jsonwebtoken.Jwts}.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final Key signingKey;

    public JwtTokenProvider(final JwtProperties properties) {
        this.properties = properties;
        final String secret = Objects.requireNonNull(
                properties.getAccessToken().getSecret(),
                "JWT access token secret must be configured");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public GeneratedToken generateToken(final String userId, final String username, final List<String> roles) {
        final Instant issuedAt = Instant.now();
        final Instant expiresAt = issuedAt.plus(
                properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        final List<String> roleClaims = List.copyOf(roles);
        final String token = Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(username)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, roleClaims)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
        return new GeneratedToken(token, expiresAt);
    }

    public Optional<TokenPayload> parseToken(final String token) {
        try {
            final Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            final Claims claims = jws.getBody();
            final Instant expiresAt = claims.getExpiration().toInstant();
            final String issuer = claims.getIssuer();
            final String expectedIssuer = properties.getIssuer();
            if (expectedIssuer != null && !expectedIssuer.equals(issuer)) {
                return Optional.empty();
            }
            if (expiresAt.isBefore(Instant.now())) {
                return Optional.empty();
            }
            final List<String> roles = extractRoles(claims);
            return Optional.of(new TokenPayload(
                    issuer,
                    claims.get(CLAIM_USER_ID, String.class),
                    claims.getSubject(),
                    roles,
                    expiresAt));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private List<String> extractRoles(final Claims claims) {
        final List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);
        if (rawRoles == null) {
            return List.of();
        }
        return rawRoles.stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableList());
    }

    public record TokenPayload(String issuer, String userId, String username, List<String> roles, Instant expiresAt) {
    }

    public record GeneratedToken(String token, Instant expiresAt) {
    }
}
