package com.bob.mta.common.security;

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
