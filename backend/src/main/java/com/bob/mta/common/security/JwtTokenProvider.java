package com.bob.mta.common.security;

<<<<<<< HEAD
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getAccessToken().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .addClaims(Map.of(
                        "username", username,
                        "roles", roles
                ))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationMinutes() {
        return properties.getAccessToken().getExpirationMinutes();
=======
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lightweight token provider that encodes authentication context as Base64 payload.
 * This is a placeholder implementation until a full JWT library is introduced.
 */
@Component
public class JwtTokenProvider {

    private static final String DELIMITER = ":";

    private final JwtProperties properties;

    public JwtTokenProvider(final JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(final String userId, final String username, final String role) {
        final Instant expiresAt = Instant.now().plus(properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        final String raw = String.join(DELIMITER, properties.getIssuer(), userId, username, role, Long.toString(expiresAt.toEpochMilli()));
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<TokenPayload> parseToken(final String token) {
        try {
            final String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            final String[] segments = decoded.split(DELIMITER);
            if (segments.length != 5) {
                return Optional.empty();
            }
            final long expiresAt = Long.parseLong(segments[4]);
            if (Instant.ofEpochMilli(expiresAt).isBefore(Instant.now())) {
                return Optional.empty();
            }
            return Optional.of(new TokenPayload(segments[0], segments[1], segments[2], segments[3]));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record TokenPayload(String issuer, String userId, String username, String role) {
>>>>>>> origin/main
    }
}
