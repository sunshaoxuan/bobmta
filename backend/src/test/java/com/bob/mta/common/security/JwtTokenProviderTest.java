package com.bob.mta.common.security;

<<<<<<< HEAD
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void createTokenShouldEmbedClaims() {
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        properties.getAccessToken().setExpirationMinutes(5);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.createToken("123", "admin", List.of("ROLE_ADMIN"));
        Claims claims = provider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("123");
        assertThat(claims.get("username")).isEqualTo("admin");
        assertThat((List<?>) claims.get("roles")).containsExactly("ROLE_ADMIN");
    }
}
=======
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtProperties properties;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setIssuer("bob-mta");
        properties.getAccessToken().setSecret("demo-secret");
        properties.getAccessToken().setExpirationMinutes(60);
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("token payload round trip succeeds for valid token")
    void shouldGenerateAndParseToken() {
        final String token = tokenProvider.generateToken("1", "admin", "ADMIN");

        final Optional<JwtTokenProvider.TokenPayload> payload = tokenProvider.parseToken(token);

        assertThat(payload).isPresent();
        assertThat(payload.get().username()).isEqualTo("admin");
        assertThat(payload.get().role()).isEqualTo("ADMIN");
        assertThat(payload.get().issuer()).isEqualTo("bob-mta");
    }

    @Test
    @DisplayName("parseToken returns empty for expired tokens")
    void shouldRejectExpiredTokens() {
        properties.getAccessToken().setExpirationMinutes(-1);
        final String token = tokenProvider.generateToken("1", "admin", "ADMIN");

        assertThat(tokenProvider.parseToken(token)).isEmpty();
    }
}

>>>>>>> origin/main
