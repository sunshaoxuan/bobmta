package com.bob.mta.common.security;

<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
<<<<<<< HEAD
=======
=======
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
>>>>>>> origin/main
>>>>>>> origin/main
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

<<<<<<< HEAD
=======
=======
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        final JwtProperties properties = new JwtProperties();
        properties.setIssuer("issuer");
        properties.getAccessToken().setSecret("secret");
        properties.getAccessToken().setExpirationMinutes(30);
        tokenProvider = new JwtTokenProvider(properties);
        filter = new JwtAuthenticationFilter(tokenProvider);
    }

>>>>>>> origin/main
>>>>>>> origin/main
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
    void filterShouldPopulateSecurityContextWhenTokenPresent() throws ServletException, IOException {
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        String token = provider.createToken("u-1", "admin", List.of("ROLE_ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
<<<<<<< HEAD
=======
=======
    @DisplayName("filter populates SecurityContext for valid bearer token")
    void shouldAuthenticateRequestWhenTokenPresent() throws ServletException, IOException {
        final String token = tokenProvider.generateToken("1", "admin", "ADMIN");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

>>>>>>> origin/main
>>>>>>> origin/main
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
<<<<<<< HEAD
=======
<<<<<<< HEAD
>>>>>>> origin/main
    void filterShouldSkipWhenTokenMissing() throws ServletException, IOException {
        JwtProperties properties = new JwtProperties();
        properties.getAccessToken().setSecret("a-very-long-secret-key-for-tests-1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
<<<<<<< HEAD
=======
=======
    @DisplayName("filter ignores malformed bearer tokens")
    void shouldIgnoreInvalidTokens() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

>>>>>>> origin/main
>>>>>>> origin/main
