package com.bob.mta.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
        final JwtTokenProvider.GeneratedToken token =
                tokenProvider.generateToken("1", "admin", List.of("ROLE_ADMIN"));

        final Optional<JwtTokenProvider.TokenPayload> payload = tokenProvider.parseToken(token.token());

        assertThat(payload).isPresent();
        assertThat(payload.get().username()).isEqualTo("admin");
        assertThat(payload.get().roles()).containsExactly("ROLE_ADMIN");
        assertThat(payload.get().issuer()).isEqualTo("bob-mta");
        assertThat(payload.get().expiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("parseToken returns empty for expired tokens")
    void shouldRejectExpiredTokens() {
        properties.getAccessToken().setExpirationMinutes(-1);
        final JwtTokenProvider.GeneratedToken token =
                tokenProvider.generateToken("1", "admin", List.of("ROLE_ADMIN"));

        assertThat(tokenProvider.parseToken(token.token())).isEmpty();
    }
}
