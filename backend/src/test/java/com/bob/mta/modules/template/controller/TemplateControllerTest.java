package com.bob.mta.modules.template.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.i18n.InMemoryMultilingualTextRepository;
import com.bob.mta.common.i18n.MultilingualTextPayload;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.i18n.InMemoryLocaleSettingsRepository;
import com.bob.mta.i18n.LocalePreferenceService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateControllerTest {

    private TemplateController controller;
    private InMemoryTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new InMemoryTemplateService(new MultilingualTextService(new InMemoryMultilingualTextRepository()));
        AuditRecorder recorder = new AuditRecorder(new InMemoryAuditService(), new ObjectMapper());
        LocalePreferenceService localePreferenceService =
                new LocalePreferenceService(new InMemoryLocaleSettingsRepository());
        controller = new TemplateController(templateService, recorder, localePreferenceService);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateTemplate() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.LINK);
        request.setName(payload("monitor"));
        request.setContent(payload("https://monitor/{{id}}"));

        ApiResponse<TemplateResponse> response = controller.create(request, "ja-JP");
        assertThat(response.getData().getName().getTranslations().get("ja-jp")).isEqualTo("monitor");
    }

    @Test
    void shouldRenderTemplate() {
        var created = controller.create(buildRequest(), "ja-JP");
        ApiResponse<RenderedTemplateResponse> rendered = controller.render(created.getData().getId(), null, "ja-JP");
        assertThat(rendered.getData().getContent()).contains("{{name}}");
        assertThat(rendered.getData().getMetadata()).isEmpty();
        assertThat(rendered.getData().getAttachmentFileName()).isNull();
    }

    @Test
    void shouldUpdateTemplate() {
        var created = controller.create(buildRequest(), "ja-JP");
        UpdateTemplateRequest update = new UpdateTemplateRequest();
        update.setName(payload("updated"));
        update.setContent(payload("Body"));
        ApiResponse<TemplateResponse> updated = controller.update(created.getData().getId(), update, "ja-JP");
        assertThat(updated.getData().getName().getTranslations().get("ja-jp")).isEqualTo("updated");
    }

    @Test
    void shouldRenderRemoteTemplateWithRdpAttachment() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.REMOTE);
        request.setName(payload("Remote Desktop"));
        request.setContent(payload("Connect {{host}}"));
        request.setEndpoint("rdp://192.168.1.10?username=ops");

        ApiResponse<TemplateResponse> created = controller.create(request, "ja-JP");
        ApiResponse<RenderedTemplateResponse> rendered = controller.render(created.getData().getId(), null, "ja-JP");

        assertThat(rendered.getData().getAttachmentFileName()).endsWith(".rdp");
        assertThat(rendered.getData().getAttachmentContent()).contains("full address");
        assertThat(rendered.getData().getMetadata().get("protocol")).isEqualTo("RDP");
        assertThat(rendered.getData().getMetadata().get("host")).isEqualTo("192.168.1.10");
        assertThat(rendered.getData().getMetadata().get("username")).isEqualTo("ops");
    }

    private CreateTemplateRequest buildRequest() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setType(TemplateType.EMAIL);
        request.setName(payload("Greeting"));
        request.setSubject(payload("Hello"));
        request.setContent(payload("Hi {{name}}"));
        request.setDescription(payload("Description"));
        return request;
    }

    private MultilingualTextPayload payload(String value) {
        return new MultilingualTextPayload("ja-JP", Map.of(
                "ja-JP", value,
                "zh-CN", value
        ));
    }
}
