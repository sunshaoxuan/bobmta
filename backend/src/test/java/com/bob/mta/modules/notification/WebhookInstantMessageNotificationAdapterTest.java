package com.bob.mta.modules.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WebhookInstantMessageNotificationAdapterTest {

    @Test
    void shouldDispatchWebhookWithRecipients() {
        RestTemplate restTemplate = new RestTemplate();
        NotificationProperties properties = new NotificationProperties();
        properties.getInstantMessage().setEnabled(true);
        properties.getInstantMessage().setWebhookUrl("https://chat.example.com/webhook");

        WebhookInstantMessageNotificationAdapter adapter =
                new WebhookInstantMessageNotificationAdapter(restTemplate, properties);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("https://chat.example.com/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{" +
                        "\"msgtype\":\"text\"," +
                        "\"text\":{\"content\":\"hello\",\"mentioned_list\":[\"alice\"]}}"))
                .andRespond(withSuccess());

        NotificationResult result = adapter.send(new InstantMessage(List.of("alice"), "hello"));

        server.verify();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata()).containsEntry("recipients", "alice");
    }

    @Test
    void shouldFailWhenWebhookMissing() {
        RestTemplate restTemplate = new RestTemplate();
        NotificationProperties properties = new NotificationProperties();
        properties.getInstantMessage().setEnabled(true);

        WebhookInstantMessageNotificationAdapter adapter =
                new WebhookInstantMessageNotificationAdapter(restTemplate, properties);

        NotificationResult result = adapter.send(new InstantMessage(List.of(), "hello"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("not configured");
    }
}
