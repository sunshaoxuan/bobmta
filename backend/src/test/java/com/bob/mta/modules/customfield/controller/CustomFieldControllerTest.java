package com.bob.mta.modules.customfield.controller;

import com.bob.mta.support.TestDatabaseHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class CustomFieldControllerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bobmta")
            .withUsername("bobmta")
            .withPassword("secret");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void migrateDatabase() {
        flyway.clean();
        flyway.migrate();
        TestDatabaseHelper.seedDefaultUsers(jdbcTemplate, passwordEncoder);
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_custom_field_value, mt_custom_field_definition RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE mt_audit_log RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("custom field definitions and values are persisted with audit logs")
    void shouldManageCustomFields() throws Exception {
        String token = authenticate();

        String definitionRequest = objectMapper.writeValueAsString(Map.of(
                "code", "ticket_url",
                "label", "チケットURL",
                "type", "TEXT",
                "required", false,
                "options", List.of(),
                "description", "サポートチケットのURL"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/custom-fields")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(definitionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ticket_url"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long fieldId = created.path("data").path("id").asLong();
        assertThat(fieldId).isPositive();

        String updateRequest = objectMapper.writeValueAsString(Map.of(
                "label", "サポートチケットURL",
                "type", "TEXT",
                "required", true,
                "options", List.of(),
                "description", "サポート窓口のチケットURL"));

        mockMvc.perform(put("/api/v1/custom-fields/" + fieldId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("サポートチケットURL"));

        String valueRequest = objectMapper.writeValueAsString(List.of(Map.of(
                "fieldId", fieldId,
                "value", "https://support.example.com/tickets/12345")));

        mockMvc.perform(put("/api/v1/custom-fields/customers/101")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("https://support.example.com/tickets/12345"));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("entityType", "CustomField"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("CREATE_CUSTOM_FIELD"));
    }

    private String authenticate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "admin",
                                "password", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String token = body.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
