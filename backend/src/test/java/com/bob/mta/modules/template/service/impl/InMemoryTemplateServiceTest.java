package com.bob.mta.modules.template.service.impl;

import com.bob.mta.modules.template.domain.TemplateType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTemplateServiceTest {

    private final InMemoryTemplateService service = new InMemoryTemplateService();

    @Test
    void shouldRenderTemplate() {
        var template = service.create(TemplateType.EMAIL, "通知", "Hi {{name}}", "正文 {{name}}", null, null, null, true, null);
        var rendered = service.render(template.getId(), Map.of("name", "Bob"));
        assertThat(rendered.getSubject()).contains("Bob");
        assertThat(rendered.getContent()).contains("Bob");
    }

    @Test
    void shouldListByType() {
        service.create(TemplateType.IM, "IM", "", "{{message}}", null, null, null, true, null);
        assertThat(service.list(TemplateType.IM)).isNotEmpty();
    }
}
