package com.bob.mta.modules.template.service;

import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;

import java.util.List;
import java.util.Map;

public interface TemplateService {

    List<TemplateDefinition> list(TemplateType type);

    TemplateDefinition get(long id);

    TemplateDefinition create(TemplateType type, String name, String subject, String content, List<String> to,
                              List<String> cc, String endpoint, boolean enabled, String description);

    TemplateDefinition update(long id, String name, String subject, String content, List<String> to, List<String> cc,
                              String endpoint, boolean enabled, String description);

    void delete(long id);

    RenderedTemplate render(long id, Map<String, String> context);
}
