package com.bob.mta.common.security;

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
