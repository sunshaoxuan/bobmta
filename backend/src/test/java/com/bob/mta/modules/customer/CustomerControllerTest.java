package com.bob.mta.modules.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("customers listing returns paginated records")
    void shouldListCustomers() throws Exception {
        final String token = authenticate();

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].code").value("CUST-101"));
    }

    @Test
    @DisplayName("customers listing requires authentication")
    void shouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("customers listing supports filtering by region")
    void shouldFilterByRegion() throws Exception {
        final String token = authenticate();

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .param("region", "北海道"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].region").value("北海道"));
    }

    @Test
    @DisplayName("customer detail returns 404 when not found")
    void shouldReturnNotFoundForMissingCustomer() throws Exception {
        final String token = authenticate();

        mockMvc.perform(get("/api/v1/customers/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4040));
    }

    private String authenticate() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        final String token = body.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}

