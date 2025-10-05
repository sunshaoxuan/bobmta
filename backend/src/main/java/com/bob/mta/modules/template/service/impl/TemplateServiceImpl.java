package com.bob.mta.modules.template.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MultilingualText;
import com.bob.mta.common.i18n.MultilingualTextScope;
import com.bob.mta.common.i18n.MultilingualTextService;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;
import com.bob.mta.modules.template.persistence.TemplateEntity;
import com.bob.mta.modules.template.repository.TemplateRepository;
import com.bob.mta.modules.template.service.TemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repository;
    private final MultilingualTextService multilingualTextService;
    private final Map<Long, TemplateDefinition> cache = new ConcurrentHashMap<>();

    public TemplateServiceImpl(TemplateRepository repository, MultilingualTextService multilingualTextService) {
        this.repository = repository;
        this.multilingualTextService = multilingualTextService;
    }

    @Override
    public List<TemplateDefinition> list(TemplateType type, Locale locale) {
        String localeTag = locale == null ? null : locale.toLanguageTag();
        return repository.findAll(type).stream()
                .map(this::toDefinition)
                .sorted((a, b) -> a.getName().getValueOrDefault(localeTag)
                        .compareToIgnoreCase(b.getName().getValueOrDefault(localeTag)))
                .toList();
    }

    @Override
    public TemplateDefinition get(long id, Locale locale) {
        return require(id);
    }

    @Override
    public TemplateDefinition create(TemplateType type, MultilingualText name, MultilingualText subject,
                                     MultilingualText content, List<String> to, List<String> cc,
                                     String endpoint, boolean enabled, MultilingualText description) {
        OffsetDateTime now = OffsetDateTime.now();
        TemplateEntity entity = new TemplateEntity();
        entity.setType(type);
        entity.setToRecipients(normalizeRecipients(to));
        entity.setCcRecipients(normalizeRecipients(cc));
        entity.setEndpoint(trimToNull(endpoint));
        entity.setEnabled(enabled);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        TemplateEntity inserted = repository.insert(entity);
        long id = inserted.getId();
        storeText(id, "name", name);
        storeText(id, "subject", subject);
        storeText(id, "content", content);
        storeText(id, "description", description);
        cache.remove(id);
        return require(id);
    }

    @Override
    public TemplateDefinition update(long id, MultilingualText name, MultilingualText subject, MultilingualText content,
                                     List<String> to, List<String> cc, String endpoint, boolean enabled,
                                     MultilingualText description) {
        TemplateDefinition existing = require(id);
        TemplateEntity entity = new TemplateEntity();
        entity.setId(id);
        entity.setType(existing.getType());
        entity.setToRecipients(normalizeRecipients(to));
        entity.setCcRecipients(normalizeRecipients(cc));
        entity.setEndpoint(trimToNull(endpoint));
        entity.setEnabled(enabled);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.update(entity);
        storeText(id, "name", name);
        storeText(id, "subject", subject);
        storeText(id, "content", content);
        storeText(id, "description", description);
        cache.remove(id);
        return require(id);
    }

    @Override
    public void delete(long id) {
        require(id);
        repository.delete(id);
        cache.remove(id);
    }

    @Override
    public RenderedTemplate render(long id, Map<String, String> context, Locale locale) {
        TemplateDefinition definition = require(id);
        if (!definition.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    Localization.text(LocalizationKeys.Errors.TEMPLATE_DISABLED));
        }
        Map<String, String> safeContext = normalizeContext(context);
        String localeTag = locale == null ? null : locale.toLanguageTag();
        String subject = replacePlaceholders(definition.getSubject() == null ? null
                : definition.getSubject().getValueOrDefault(localeTag), safeContext);
        String contentValue = replacePlaceholders(definition.getContent() == null ? null
                : definition.getContent().getValueOrDefault(localeTag), safeContext);
        List<String> toRecipients = definition.getTo().stream()
                .map(value -> replacePlaceholders(value, safeContext))
                .toList();
        List<String> ccRecipients = definition.getCc().stream()
                .map(value -> replacePlaceholders(value, safeContext))
                .toList();
        String endpoint = replacePlaceholders(definition.getEndpoint(), safeContext);

        RemoteArtifact artifact = definition.getType() == TemplateType.REMOTE
                ? buildRemoteArtifact(definition, endpoint, localeTag)
                : RemoteArtifact.empty();

        return new RenderedTemplate(subject, contentValue, toRecipients, ccRecipients, endpoint,
                artifact.fileName(), artifact.content(), artifact.contentType(), artifact.metadata());
    }

    private TemplateDefinition toDefinition(TemplateEntity entity) {
        long id = Optional.ofNullable(entity.getId()).orElseThrow(() ->
                new IllegalStateException("Template entity missing id"));
        return cache.computeIfAbsent(id, ignored -> {
            MultilingualText name = loadText(id, "name")
                    .orElseThrow(() -> new IllegalStateException("Template name missing for id=" + id));
            MultilingualText subject = loadText(id, "subject").orElse(null);
            MultilingualText content = loadText(id, "content").orElse(null);
            MultilingualText description = loadText(id, "description").orElse(null);
            List<String> toRecipients = entity.getToRecipients() == null ? List.of()
                    : List.copyOf(entity.getToRecipients());
            List<String> ccRecipients = entity.getCcRecipients() == null ? List.of()
                    : List.copyOf(entity.getCcRecipients());
            return new TemplateDefinition(id, entity.getType(), name, subject, content, toRecipients, ccRecipients,
                    entity.getEndpoint(), entity.isEnabled(), description, entity.getCreatedAt(), entity.getUpdatedAt());
        });
    }

    private TemplateDefinition require(long id) {
        return repository.findById(id)
                .map(this::toDefinition)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private void storeText(long id, String field, MultilingualText text) {
        if (text == null) {
            return;
        }
        multilingualTextService.upsert(MultilingualTextScope.TEMPLATE_DEFINITION, String.valueOf(id), field, text);
    }

    private Optional<MultilingualText> loadText(long id, String field) {
        return multilingualTextService.find(MultilingualTextScope.TEMPLATE_DEFINITION, String.valueOf(id), field);
    }

    private List<String> normalizeRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return recipients.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private Map<String, String> normalizeContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        context.forEach((key, value) -> normalized.put(key, value == null ? "" : value));
        return normalized;
    }

    private RemoteArtifact buildRemoteArtifact(TemplateDefinition definition, String endpoint, String localeTag) {
        if (!StringUtils.hasText(endpoint)) {
            return RemoteArtifact.empty();
        }
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("protocol", scheme.isEmpty() ? "UNKNOWN" : scheme.toUpperCase(Locale.ROOT));
            metadata.put("endpoint", endpoint);
            Map<String, String> query = parseQuery(uri.getQuery());
            String host = uri.getHost();
            if (host != null) {
                metadata.put("host", host);
            }
            if (uri.getPort() > 0) {
                metadata.put("port", String.valueOf(uri.getPort()));
            }
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                metadata.put("username", userInfo);
            } else if (query.containsKey("username")) {
                metadata.put("username", query.get("username"));
            }

            if ("rdp".equals(scheme) && host != null) {
                String fileName = definition.getName().getValueOrDefault(localeTag)
                        .replaceAll("\\s+", "-").toLowerCase(Locale.ROOT) + ".rdp";
                int port = uri.getPort() > 0 ? uri.getPort() : 3389;
                String username = metadata.getOrDefault("username", "");
                StringBuilder builder = new StringBuilder();
                builder.append("full address:s:").append(host);
                if (port != 3389) {
                    builder.append(":").append(port);
                }
                builder.append("\nusername:s:").append(username);
                builder.append("\nprompt for credentials:i:1\nauthentication level:i:2\nredirectclipboard:i:1\n");
                metadata.put("port", String.valueOf(port));
                return new RemoteArtifact(fileName, builder.toString(), "application/x-rdp", metadata);
            }

            if (("ssh".equals(scheme) || "sftp".equals(scheme)) && host != null) {
                String username = metadata.getOrDefault("username", "");
                StringBuilder command = new StringBuilder("ssh ");
                if (!username.isEmpty()) {
                    command.append(username).append("@");
                }
                command.append(host);
                if (uri.getPort() > 0) {
                    command.append(" -p ").append(uri.getPort());
                }
                metadata.put("command", command.toString());
            }

            return new RemoteArtifact(null, null, null, metadata);
        } catch (IllegalArgumentException ex) {
            return new RemoteArtifact(null, null, null, Map.of(
                    "protocol", "UNKNOWN",
                    "endpoint", endpoint,
                    "error", Localization.text(LocalizationKeys.Errors.TEMPLATE_ENDPOINT_INVALID)));
        }
    }

    private Map<String, String> parseQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key;
            String value;
            if (idx >= 0) {
                key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            } else {
                key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                value = "";
            }
            values.put(key, value);
        }
        return values;
    }

    private record RemoteArtifact(String fileName, String content, String contentType, Map<String, String> metadata) {

        static RemoteArtifact empty() {
            return new RemoteArtifact(null, null, null, Map.of());
        }
    }
}
