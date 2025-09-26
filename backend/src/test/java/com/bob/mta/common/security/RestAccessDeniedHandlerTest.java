package com.bob.mta.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("forbidden access returns API error envelope")
    void shouldWriteForbiddenResponse() throws IOException {
        final MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        final JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(4031);
        assertThat(body.path("message").asText()).isNotBlank();
    }
}

