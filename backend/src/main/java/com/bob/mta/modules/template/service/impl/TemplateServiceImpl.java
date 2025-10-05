package com.bob.mta.modules.template.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.i18n.MultilingualText;
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
import java.util.function.Consumer;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repository;
    private final Map<Long, TemplateDefinition> cache = new ConcurrentHashMap<>();

    public TemplateServiceImpl(TemplateRepository repository) {
        this.repository = repository;
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
        if (name == null) {
            throw new IllegalArgumentException("Template name is required");
        }
        OffsetDateTime now = OffsetDateTime.now();
        TemplateEntity entity = new TemplateEntity();
        entity.setType(type);
        entity.setToRecipients(normalizeRecipients(to));
        entity.setCcRecipients(normalizeRecipients(cc));
        entity.setEndpoint(trimToNull(endpoint));
        entity.setEnabled(enabled);
        applyText(name, entity::setNameDefaultLocale, entity::setNameTranslations);
        applyText(subject, entity::setSubjectDefaultLocale, entity::setSubjectTranslations);
        applyText(content, entity::setContentDefaultLocale, entity::setContentTranslations);
        applyText(description, entity::setDescriptionDefaultLocale, entity::setDescriptionTranslations);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        TemplateEntity inserted = repository.insert(entity);
        return toDefinition(inserted);
    }

    @Override
    public TemplateDefinition update(long id, MultilingualText name, MultilingualText subject, MultilingualText content,
                                     List<String> to, List<String> cc, String endpoint, boolean enabled,
                                     MultilingualText description) {
        TemplateDefinition existing = require(id);
        if (name == null) {
            throw new IllegalArgumentException("Template name is required");
        }
        TemplateEntity entity = new TemplateEntity();
        entity.setId(id);
        entity.setType(existing.getType());
        entity.setToRecipients(normalizeRecipients(to));
        entity.setCcRecipients(normalizeRecipients(cc));
        entity.setEndpoint(trimToNull(endpoint));
        entity.setEnabled(enabled);
        applyText(name, entity::setNameDefaultLocale, entity::setNameTranslations);
        applyText(subject, entity::setSubjectDefaultLocale, entity::setSubjectTranslations);
        applyText(content, entity::setContentDefaultLocale, entity::setContentTranslations);
        applyText(description, entity::setDescriptionDefaultLocale, entity::setDescriptionTranslations);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setUpdatedAt(OffsetDateTime.now());
        TemplateEntity updated = repository.update(entity);
        cache.remove(id);
        return toDefinition(updated);
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
        TemplateDefinition definition = buildDefinition(entity, id);
        cache.put(id, definition);
        return definition;
    }

    private TemplateDefinition require(long id) {
        return cache.computeIfAbsent(id, key -> repository.findById(key)
                .map(entity -> buildDefinition(entity, key))
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND)));
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

    private void applyText(MultilingualText text,
                           Consumer<String> defaultLocaleSetter,
                           Consumer<Map<String, String>> translationsSetter) {
        if (text == null) {
            defaultLocaleSetter.accept(null);
            translationsSetter.accept(Map.of());
        } else {
            defaultLocaleSetter.accept(text.getDefaultLocale());
            translationsSetter.accept(text.getTranslations());
        }
    }

    private TemplateDefinition buildDefinition(TemplateEntity entity, long id) {
        MultilingualText name = toText(entity.getNameDefaultLocale(), entity.getNameTranslations())
                .orElseThrow(() -> new IllegalStateException("Template name missing for id=" + id));
        MultilingualText subject = toText(entity.getSubjectDefaultLocale(), entity.getSubjectTranslations()).orElse(null);
        MultilingualText content = toText(entity.getContentDefaultLocale(), entity.getContentTranslations()).orElse(null);
        MultilingualText description = toText(entity.getDescriptionDefaultLocale(), entity.getDescriptionTranslations()).orElse(null);
        List<String> toRecipients = entity.getToRecipients() == null ? List.of()
                : List.copyOf(entity.getToRecipients());
        List<String> ccRecipients = entity.getCcRecipients() == null ? List.of()
                : List.copyOf(entity.getCcRecipients());
        return new TemplateDefinition(id, entity.getType(), name, subject, content, toRecipients, ccRecipients,
                entity.getEndpoint(), entity.isEnabled(), description, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Optional<MultilingualText> toText(String defaultLocale, Map<String, String> translations) {
        if (!StringUtils.hasText(defaultLocale) || translations == null || translations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MultilingualText.of(defaultLocale, translations));
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
