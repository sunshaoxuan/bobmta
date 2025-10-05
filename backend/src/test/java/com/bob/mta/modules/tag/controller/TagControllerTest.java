package com.bob.mta.modules.tag.controller;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("tag lifecycle persists assignments and audit records")
    void shouldCreateAssignAndAuditTag() throws Exception {
        String token = authenticate();

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "name", Map.of(
                        "defaultLocale", "ja-JP",
                        "translations", Map.of(
                                "ja-JP", "緊急対応",
                                "zh-CN", "紧急处理")),
                "color", "#F5222D",
                "icon", "AlertOutlined",
                "scope", "CUSTOMER",
                "applyRule", null,
                "enabled", true));

        MvcResult createResult = mockMvc.perform(post("/api/v1/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name.translations.ja-jp").value("緊急対応"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long tagId = created.path("data").path("id").asLong();
        assertThat(tagId).isPositive();

        String assignmentBody = objectMapper.writeValueAsString(Map.of(
                "entityType", "CUSTOMER",
                "entityId", "101"));

        mockMvc.perform(post("/api/v1/tags/" + tagId + "/assignments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityId").value("101"));

        mockMvc.perform(get("/api/v1/tags/" + tagId + "/assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].entityId").value("101"));

        mockMvc.perform(delete("/api/v1/tags/" + tagId + "/assignments/CUSTOMER/101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("entityType", "Tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("CREATE_TAG"));
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
