package com.bob.mta.modules.template.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.MessageResolver;
import com.bob.mta.common.i18n.TestMessageResolverFactory;
import com.bob.mta.modules.audit.service.AuditRecorder;
import com.bob.mta.modules.audit.service.impl.InMemoryAuditService;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.dto.CreateTemplateRequest;
import com.bob.mta.modules.template.dto.RenderedTemplateResponse;
import com.bob.mta.modules.template.dto.TemplateResponse;
import com.bob.mta.modules.template.dto.UpdateTemplateRequest;
import com.bob.mta.modules.template.service.impl.InMemoryTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateControllerTest {

    private TemplateController controller;
    private InMemoryTemplateService templateService;
    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        templateService = new InMemoryTemplateService();
        AuditRecorder recorder = new AuditRecorder(new InMemoryAuditService(), new ObjectMapper());
        messageResolver = TestMessageResolverFactory.create();
        controller = new TemplateController(templateService, recorder, messageResolver);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldCreateTemplate() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.LINK);
        request.setName("监控");
        request.setContent("https://monitor/{{id}}");

        ApiResponse<TemplateResponse> response = controller.create(request);
        assertThat(response.getData().getName()).isEqualTo("监控");
    }

    @Test
    void shouldRenderTemplate() {
        var created = controller.create(buildRequest());
        ApiResponse<RenderedTemplateResponse> rendered = controller.render(created.getData().getId(), null);
        assertThat(rendered.getData().getContent()).contains("{{name}}");
        assertThat(rendered.getData().getMetadata()).isEmpty();
        assertThat(rendered.getData().getAttachmentFileName()).isNull();
    }

    @Test
    void shouldUpdateTemplate() {
        var created = controller.create(buildRequest());
        UpdateTemplateRequest update = new UpdateTemplateRequest();
        update.setName("更新后");
        update.setContent("Body");
        ApiResponse<TemplateResponse> updated = controller.update(created.getData().getId(), update);
        assertThat(updated.getData().getName()).isEqualTo("更新后");
    }

    @Test
    void shouldRenderRemoteTemplateWithRdpAttachment() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.REMOTE);
        request.setName("远程桌面");
        request.setContent("连接 {{host}}");
        request.setEndpoint("rdp://192.168.1.10?username=ops");

        ApiResponse<TemplateResponse> created = controller.create(request);
        ApiResponse<RenderedTemplateResponse> rendered = controller.render(created.getData().getId(), null);

        assertThat(rendered.getData().getAttachmentFileName()).endsWith(".rdp");
        assertThat(rendered.getData().getAttachmentContent()).contains("full address");
        assertThat(rendered.getData().getMetadata().get("protocol")).isEqualTo("RDP");
        assertThat(rendered.getData().getMetadata().get("host")).isEqualTo("192.168.1.10");
        assertThat(rendered.getData().getMetadata().get("username")).isEqualTo("ops");
    }

    private CreateTemplateRequest buildRequest() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.EMAIL);
        request.setName("问候");
        request.setSubject("Hello");
        request.setContent("Hi {{name}}");
        return request;
    }
}
