package com.bob.mta.modules.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.ACCEPTED;

class HttpApiNotificationAdapterTest {

    @Test
    void shouldInvokeRemoteEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        HttpApiNotificationAdapter adapter = new HttpApiNotificationAdapter(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://workflow.example.com"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(ACCEPTED).body("{\"status\":\"ok\"}"));

        ApiCallRequest request = new ApiCallRequest("https://workflow.example.com", "POST",
                "{\"ticket\":42}", Map.of("X-Correlation-Id", "123"));
        NotificationResult result = adapter.invoke(request);

        server.verify();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata()).containsEntry("status", String.valueOf(ACCEPTED.value()));
    }

    @Test
    void shouldReturnFailureWhenHttpError() {
        RestTemplate restTemplate = new RestTemplate();
        HttpApiNotificationAdapter adapter = new HttpApiNotificationAdapter(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://workflow.example.com"))
                .andRespond(withServerError());

        ApiCallRequest request = new ApiCallRequest("https://workflow.example.com", "POST", null, Map.of());
        NotificationResult result = adapter.invoke(request);
        assertThat(result.isSuccess()).isFalse();
    }
}
