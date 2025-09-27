package com.bob.mta.modules.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("plan lifecycle supports creation, publish and ICS export")
    void shouldManagePlanLifecycle() throws Exception {
        String token = authenticate();
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.plusHours(2);

        Map<String, Object> payload = Map.of(
                "tenantId", "tenant-test",
                "title", "演练计划",
                "description", "验证巡检流程",
                "customerId", "cust-777",
                "owner", "admin",
                "startTime", start,
                "endTime", end,
                "timezone", "Asia/Tokyo",
                "participants", List.of("admin", "operator"),
                "nodes", List.of(Map.of(
                        "name", "准备环境",
                        "type", "CHECKLIST",
                        "assignee", "admin",
                        "order", 1,
                        "expectedDurationMinutes", 60,
                        "children", List.of()
                ))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("演练计划"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String planId = created.path("data").path("id").asText();
        assertThat(planId).isNotBlank();

        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/v1/plans/" + planId + "/ics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/calendar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")));
    }

    @Test
    @DisplayName("plans listing exposes summary payloads with pagination")
    void shouldListPlans() throws Exception {
        String token = authenticate();

        mockMvc.perform(get("/api/v1/plans")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.list[0].id").isNotEmpty());
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
