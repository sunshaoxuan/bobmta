package com.bob.mta.modules.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MinioFileIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MinioFileIntegrationTest.class);
    private static final String MINIO_IMAGE = "quay.io/minio/minio:RELEASE.2024-03-30T09-41-56Z";
    private static final String MINIO_USER = "minio";
    private static final String MINIO_PASSWORD = "minio123456";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withEnv("MINIO_ROOT_USER", MINIO_USER)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
            .withCommand("server /data --console-address :9001")
            .withExposedPorts(9000, 9001)
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.file.storage", () -> "MINIO");
        registry.add("app.file.minio.endpoint", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("app.file.minio.access-key", () -> MINIO_USER);
        registry.add("app.file.minio.secret-key", () -> MINIO_PASSWORD);
        registry.add("app.file.minio.download-expiry-seconds", () -> 300);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MinioClient minioClient;

    @Test
    @DisplayName("plan attachments leverage MinIO storage end-to-end")
    void shouldHandleAttachmentLifecycleWithMinio() throws Exception {
        String token = authenticate();

        byte[] attachmentContent = "Checklist evidence".getBytes(StandardCharsets.UTF_8);
        JsonNode attachmentMetadata = registerFile(token, "plan-attachments", "PLAN_NODE", "node-alpha",
                "evidence.txt", "text/plain", attachmentContent.length);
        String attachmentFileId = attachmentMetadata.path("data").path("id").asText();
        String attachmentBucket = attachmentMetadata.path("data").path("bucket").asText();
        String attachmentObjectKey = attachmentMetadata.path("data").path("objectKey").asText();
        assertThat(attachmentFileId).isNotBlank();
        uploadObject(attachmentBucket, attachmentObjectKey, attachmentContent, "text/plain");

        byte[] staleContent = "to be removed".getBytes(StandardCharsets.UTF_8);
        JsonNode staleMetadata = registerFile(token, "temp-files", "TEMP", "cleanup",
                "old.txt", "text/plain", staleContent.length);
        String staleFileId = staleMetadata.path("data").path("id").asText();
        String staleBucket = staleMetadata.path("data").path("bucket").asText();
        String staleObjectKey = staleMetadata.path("data").path("objectKey").asText();
        uploadObject(staleBucket, staleObjectKey, staleContent, "text/plain");

        mockMvc.perform(delete("/api/v1/files/" + staleFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/files/" + staleFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        assertThatThrownBy(() -> minioClient.statObject(StatObjectArgs.builder()
                        .bucket(staleBucket)
                        .object(staleObjectKey)
                        .build()))
                .isInstanceOf(ErrorResponseException.class);

        JsonNode plan = createPlan(token);
        String planId = plan.path("data").path("id").asText();
        assertThat(planId).isNotBlank();

        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/plans/" + planId + "/nodes/node-alpha/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("operatorId", "admin"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/plans/" + planId + "/nodes/node-alpha/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "operatorId", "admin",
                                "resultSummary", "Completed",
                                "log", "All checks passed",
                                "fileIds", List.of(attachmentFileId)))))
                .andExpect(status().isOk());

        MvcResult detailResult = mockMvc.perform(get("/api/v1/plans/" + planId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode attachments = detail.path("data").path("nodes").get(0).path("execution").path("attachments");
        assertThat(attachments).isNotNull();
        assertThat(attachments.isArray()).isTrue();
        assertThat(attachments).hasSize(1);
        JsonNode attachment = attachments.get(0);
        assertThat(attachment.path("id").asText()).isEqualTo(attachmentFileId);
        assertThat(attachment.path("name").asText()).isEqualTo("evidence.txt");
        String downloadUrl = attachment.path("downloadUrl").asText();
        assertThat(downloadUrl).isNotBlank();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(downloadResponse.statusCode()).isEqualTo(200);
        assertThat(new String(downloadResponse.body(), StandardCharsets.UTF_8)).isEqualTo("Checklist evidence");

        MvcResult timelineResult = mockMvc.perform(get("/api/v1/plans/" + planId + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode timeline = objectMapper.readTree(timelineResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
        assertThat(timeline.isArray()).isTrue();
        boolean nodeCompleted = false;
        for (JsonNode event : timeline) {
            if ("NODE_COMPLETED".equals(event.path("type").asText())) {
                nodeCompleted = true;
                break;
            }
        }
        assertThat(nodeCompleted).isTrue();

        MvcResult listResult = mockMvc.perform(get("/api/v1/files")
                        .header("Authorization", "Bearer " + token)
                        .param("bizType", "PLAN_NODE")
                        .param("bizId", "node-alpha"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listBody = objectMapper.readTree(listResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode listData = listBody.path("data");
        assertThat(listData.isArray()).isTrue();
        boolean foundAttachment = false;
        for (JsonNode node : listData) {
            if (attachmentFileId.equals(node.path("id").asText())) {
                assertThat(node.path("downloadUrl").asText()).isNotBlank();
                foundAttachment = true;
            }
        }
        assertThat(foundAttachment).isTrue();
    }

    private JsonNode registerFile(String token, String bucket, String bizType, String bizId,
                                  String fileName, String contentType, long size) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName);
        payload.put("contentType", contentType);
        payload.put("size", size);
        payload.put("bucket", bucket);
        payload.put("bizType", bizType);
        payload.put("bizId", bizId);
        MvcResult result = mockMvc.perform(post("/api/v1/files")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void uploadObject(String bucket, String objectKey, byte[] content, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload object {} in bucket {}", objectKey, bucket, e);
            throw new IllegalStateException(e);
        }
    }

    private JsonNode createPlan(String token) throws Exception {
        Map<String, Object> node = new HashMap<>();
        node.put("id", "node-alpha");
        node.put("name", "Initial checks");
        node.put("type", "CHECKLIST");
        node.put("assignee", "operator");
        node.put("order", 1);
        node.put("expectedDurationMinutes", 30);
        node.put("actionType", "NONE");
        node.put("completionThreshold", 100);
        node.put("children", List.of());

        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "tenant-files");
        payload.put("title", "Plan with attachments");
        payload.put("description", "Verify file storage integration");
        payload.put("customerId", "cust-files");
        payload.put("owner", "admin");
        payload.put("startTime", OffsetDateTime.now().plusDays(1));
        payload.put("endTime", OffsetDateTime.now().plusDays(1).plusHours(2));
        payload.put("timezone", "Asia/Tokyo");
        payload.put("participants", List.of("admin", "operator"));
        payload.put("nodes", List.of(node));

        MvcResult result = mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
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
