package com.bob.mta.modules.template.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.service.TemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryTemplateService implements TemplateService {

    private final AtomicLong idGenerator = new AtomicLong(500);
    private final Map<Long, TemplateDefinition> definitions = new ConcurrentHashMap<>();

    public InMemoryTemplateService() {
        seedDefaults();
    }

    private void seedDefaults() {
        create(TemplateType.EMAIL, "客户巡检通知", "【{{customer_name}}】巡检安排", "尊敬的{{customer_name}}，我们将在{{schedule_date}}进行巡检。",
                List.of("ops@customer.com"), List.of(), null, true, "标准巡检邮件模板");
        create(TemplateType.REMOTE, "SSH登录模板", null, "ssh {{username}}@{{host}}",
                List.of(), List.of(), "ssh://{{host}}", true, "常用SSH连接模板");
    }

    @Override
    public List<TemplateDefinition> list(TemplateType type) {
        return definitions.values().stream()
                .filter(def -> type == null || def.getType() == type)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Override
    public TemplateDefinition get(long id) {
        TemplateDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        return definition;
    }

    @Override
    public TemplateDefinition create(TemplateType type, String name, String subject, String content, List<String> to,
                                     List<String> cc, String endpoint, boolean enabled, String description) {
        long id = idGenerator.incrementAndGet();
        TemplateDefinition definition = new TemplateDefinition(id, type, name, subject, content, to, cc, endpoint,
                enabled, description, OffsetDateTime.now(), OffsetDateTime.now());
        definitions.put(id, definition);
        return definition;
    }

    @Override
    public TemplateDefinition update(long id, String name, String subject, String content, List<String> to, List<String> cc,
                                     String endpoint, boolean enabled, String description) {
        TemplateDefinition definition = get(id);
        TemplateDefinition updated = new TemplateDefinition(id, definition.getType(), name, subject, content, to, cc,
                endpoint, enabled, description, definition.getCreatedAt(), OffsetDateTime.now());
        definitions.put(id, updated);
        return updated;
    }

    @Override
    public void delete(long id) {
        definitions.remove(id);
    }

    @Override
    public RenderedTemplate render(long id, Map<String, String> context) {
        TemplateDefinition definition = get(id);
        if (!definition.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Template disabled");
        }
        Map<String, String> safeContext = context == null ? Map.of() : context;
        String subject = replacePlaceholders(definition.getSubject(), safeContext);
        String content = replacePlaceholders(definition.getContent(), safeContext);
        List<String> to = definition.getTo().stream().map(value -> replacePlaceholders(value, safeContext)).toList();
        List<String> cc = definition.getCc().stream().map(value -> replacePlaceholders(value, safeContext)).toList();
        String endpoint = replacePlaceholders(definition.getEndpoint(), safeContext);
        return new RenderedTemplate(subject, content, to, cc, endpoint);
    }

    private String replacePlaceholders(String template, Map<String, String> context) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }
}
