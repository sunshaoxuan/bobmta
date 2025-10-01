package com.bob.mta.modules.plan.service.impl;

import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.service.TemplateService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestTemplateService implements TemplateService {

    private final Map<Long, RenderedTemplate> registry = new ConcurrentHashMap<>();

    void register(long id, RenderedTemplate template) {
        registry.put(id, template);
    }

    @Override
    public List<TemplateDefinition> list(TemplateType type, Locale locale) {
        return List.of();
    }

    @Override
    public TemplateDefinition get(long id, Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TemplateDefinition create(TemplateType type, MultilingualText name, MultilingualText subject,
                                     MultilingualText content, List<String> to, List<String> cc, String endpoint,
                                     boolean enabled, MultilingualText description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TemplateDefinition update(long id, MultilingualText name, MultilingualText subject,
                                     MultilingualText content, List<String> to, List<String> cc, String endpoint,
                                     boolean enabled, MultilingualText description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RenderedTemplate render(long id, Map<String, String> context, Locale locale) {
        return registry.getOrDefault(id, new RenderedTemplate(null, null, List.of(), List.of(), null,
                null, null, null, Map.of()));
    }
}
