package com.bob.mta.modules.template.service.impl;

import com.bob.mta.common.i18n.InMemoryMultilingualTextRepository;
import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.repository.InMemoryTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateServiceImplTest {

    private final TemplateServiceImpl service = new TemplateServiceImpl(new InMemoryTemplateRepository(),
            new MultilingualTextService(new InMemoryMultilingualTextRepository()));

    @Test
    void shouldRenderTemplate() {
        var template = service.create(TemplateType.EMAIL, text("Notice"), text("Hi {{name}}"), text("Body {{name}}"), null, null, null, true, null);
        var rendered = service.render(template.getId(), Map.of("name", "Bob"), Locale.JAPAN);
        assertThat(rendered.getSubject()).contains("Bob");
        assertThat(rendered.getContent()).contains("Bob");
        assertThat(rendered.getMetadata()).isEmpty();
    }

    @Test
    void shouldListByType() {
        service.create(TemplateType.IM, text("IM"), text(""), text("{{message}}"), List.of("ops"), null, null, true, null);
        assertThat(service.list(TemplateType.IM, Locale.JAPAN)).isNotEmpty();
    }

    @Test
    void shouldGenerateRdpAttachmentForRemoteTemplate() {
        var template = service.create(TemplateType.REMOTE, text("Remote Desktop"), null, text("Connect {{host}}"),
                null, null, "rdp://10.0.0.5?username=svc", true, text("RDP"));

        var rendered = service.render(template.getId(), Map.of("host", "10.0.0.5"), Locale.JAPAN);

        assertThat(rendered.getAttachmentFileName()).endsWith(".rdp");
        assertThat(rendered.getAttachmentContent()).contains("full address:s:10.0.0.5");
        assertThat(rendered.getAttachmentContentType()).isEqualTo("application/x-rdp");
        assertThat(rendered.getMetadata().get("protocol")).isEqualTo("RDP");
        assertThat(rendered.getMetadata().get("username")).isEqualTo("svc");
    }

    @Test
    void shouldUpdateTemplateRecipientsAndDescription() {
        var created = service.create(TemplateType.EMAIL, text("Initial"), text("Subject"),
                text("Hello"), List.of("ops@old"), null, null, true, text("Desc"));

        service.update(created.getId(), text("Updated"), text("New subject"), text("Body"),
                List.of("ops@new"), List.of("cc@new"), "https://updated.example.com", false, text("New desc"));

        var fetched = service.get(created.getId(), Locale.JAPAN);
        assertThat(fetched.getName().getValueOrDefault("ja-JP")).isEqualTo("Updated");
        assertThat(fetched.getSubject().getValueOrDefault("ja-JP")).isEqualTo("New subject");
        assertThat(fetched.getTo()).containsExactly("ops@new");
        assertThat(fetched.getCc()).containsExactly("cc@new");
        assertThat(fetched.getEndpoint()).isEqualTo("https://updated.example.com");
        assertThat(fetched.isEnabled()).isFalse();
        assertThat(fetched.getDescription().getValueOrDefault("ja-JP")).isEqualTo("New desc");
    }

    private MultilingualText text(String value) {
        return MultilingualText.of("ja-JP", Map.of(
                "ja-JP", value,
                "zh-CN", value
        ));
    }
}
