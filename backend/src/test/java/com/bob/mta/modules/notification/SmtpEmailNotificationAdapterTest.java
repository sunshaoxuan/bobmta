package com.bob.mta.modules.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailNotificationAdapterTest {

    @Test
    void shouldSendEmailUsingJavaMailSender() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        doAnswer(invocation -> {
            MimeMessage sent = invocation.getArgument(0);
            sent.saveChanges();
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("noreply@example.com");
        properties.getEmail().setReplyTo("reply@example.com");

        SmtpEmailNotificationAdapter adapter = new SmtpEmailNotificationAdapter(mailSender, properties);

        EmailMessage request = new EmailMessage(List.of("user@example.com"), List.of("cc@example.com"),
                "Subject", "<p>Hello</p>");
        NotificationResult result = adapter.send(request);

        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        List<String> recipients = Arrays.stream(sent.getAllRecipients())
                .map(Object::toString)
                .toList();
        assertThat(recipients.toString()).contains("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Subject");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata()).containsEntry("from", "noreply@example.com");
    }

    @Test
    void shouldReturnFailureWhenFromMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setEnabled(true);

        SmtpEmailNotificationAdapter adapter = new SmtpEmailNotificationAdapter(mailSender, properties);

        NotificationResult result = adapter.send(new EmailMessage(List.of("user@example.com"), List.of(),
                "Subject", "Body"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("not configured");
    }
}
