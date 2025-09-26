package com.bob.mta.common.security;

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
    }
}
