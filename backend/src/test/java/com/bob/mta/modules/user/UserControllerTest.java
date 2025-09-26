package com.bob.mta.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void authenticate() throws Exception {
        adminToken = login("admin", "admin123");
    }

    @Test
    @DisplayName("admin can create a new user and receive activation token")
    void shouldCreateUser() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "qa.user",
                                "displayName", "QA User",
                                "email", "qa.user@example.com",
                                "password", "password123",
                                "roles", List.of("operator")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("qa.user"))
                .andExpect(jsonPath("$.data.activation.token").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACTIVATION"))
                .andReturn();

        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        final String token = node.path("data").path("activation").path("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/v1/users/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("user listing supports status filtering")
    void shouldListUsersWithFilters() throws Exception {
        // create additional pending user
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "pending.user",
                                "displayName", "Pending",
                                "email", "pending@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING_ACTIVATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].status").value("PENDING_ACTIVATION"));
    }

    @Test
    @DisplayName("activation resend requires authentication and returns token")
    void shouldResendActivation() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "resend.user",
                                "displayName", "Resend",
                                "email", "resend@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        final String userId = node.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/users/" + userId + "/activation/resend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("roles assignment updates user profile")
    void shouldAssignRoles() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "role.user",
                                "displayName", "Role User",
                                "email", "role.user@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        final String userId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(post("/api/v1/users/" + userId + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("roles", List.of("admin", "auditor")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[1]").value("AUDITOR"));
    }

    private String login(final String username, final String password) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        final String token = node.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
