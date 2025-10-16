package com.bob.mta.modules.audit.controller;

import com.bob.mta.modules.audit.domain.AuditLog;
import com.bob.mta.modules.audit.service.AuditService;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class AuditControllerIntegrationTest {

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
    private AuditService auditService;

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
    void cleanAuditLog() {
        jdbcTemplate.execute("TRUNCATE TABLE mt_audit_log RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("audit controller returns filtered audit records")
    void shouldFilterAuditLogs() throws Exception {
        auditService.record(new AuditLog(0L, OffsetDateTime.now(ZoneOffset.UTC),
                "admin", "管理员", "Tag", "55", "CREATE_TAG",
                "Created tag", null, "{\"name\":\"优先级\"}", "req-1", "127.0.0.1", "JUnit"));
        auditService.record(new AuditLog(0L, OffsetDateTime.now(ZoneOffset.UTC),
                "operator", "Operator", "CustomField", "101", "UPSERT_CUSTOM_FIELD_VALUE",
                "Updated field", null, "{\"field\":\"value\"}", "req-2", "127.0.0.1", "JUnit"));

        String token = authenticate();

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("entityType", "Tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("CREATE_TAG"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("audit controller supports filtering by user and action")
    void shouldFilterByUserAndAction() throws Exception {
        auditService.record(new AuditLog(0L, OffsetDateTime.now(ZoneOffset.UTC),
                "admin", "管理员", "Customer", "cust-001", "UPDATE_CUSTOMER",
                "Updated customer", "{\"name\":\"old\"}", "{\"name\":\"new\"}", "req-10", "192.168.0.10", "JUnit"));
        auditService.record(new AuditLog(0L, OffsetDateTime.now(ZoneOffset.UTC),
                "operator", "操作员", "Customer", "cust-002", "UPDATE_CUSTOMER",
                "Updated customer", null, null, "req-11", "192.168.0.11", "JUnit"));

        String token = authenticate();

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("entityType", "Customer")
                        .param("action", "UPDATE_CUSTOMER")
                        .param("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entityId").value("cust-001"));
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

