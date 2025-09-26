package com.bob.mta.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("unauthenticated access renders JSON envelope")
    void shouldWriteUnauthorizedResponse() throws IOException, ServletException {
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new AuthenticationException("denied") {});

        assertThat(response.getStatus()).isEqualTo(401);
        final JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(4010);
        assertThat(body.path("message").asText()).isNotEmpty();
    }
}

