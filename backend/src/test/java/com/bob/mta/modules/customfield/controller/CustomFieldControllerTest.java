package com.bob.mta.modules.customfield.controller;

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
class CustomFieldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
