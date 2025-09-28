package com.bob.mta.modules.template.service;

import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface TemplateService {

    List<TemplateDefinition> list(TemplateType type, Locale locale);

    TemplateDefinition get(long id, Locale locale);

    TemplateDefinition create(TemplateType type, MultilingualText name, MultilingualText subject, MultilingualText content, List<String> to,
                              List<String> cc, String endpoint, boolean enabled, MultilingualText description);

    TemplateDefinition update(long id, MultilingualText name, MultilingualText subject, MultilingualText content, List<String> to, List<String> cc,
                              String endpoint, boolean enabled, MultilingualText description);

    void delete(long id);

    RenderedTemplate render(long id, Map<String, String> context, Locale locale);
}
