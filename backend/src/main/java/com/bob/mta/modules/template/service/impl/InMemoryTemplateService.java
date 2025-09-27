package com.bob.mta.modules.template.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.template.domain.RenderedTemplate;
import com.bob.mta.modules.template.domain.TemplateDefinition;
import com.bob.mta.modules.template.domain.TemplateType;
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
        create(TemplateType.EMAIL,
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_EMAIL_NAME),
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_EMAIL_SUBJECT),
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_EMAIL_CONTENT),
                List.of("ops@customer.com"), List.of(), null, true,
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_EMAIL_DESCRIPTION));
        create(TemplateType.REMOTE,
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_REMOTE_NAME),
                null,
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_REMOTE_CONTENT),
                List.of(), List.of(), "rdp://{{host}}?username={{username}}", true,
                Localization.text(LocalizationKeys.Seeds.TEMPLATE_REMOTE_DESCRIPTION));
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
        Map<String, String> safeContext = normalizeContext(context);
        String subject = replacePlaceholders(definition.getSubject(), safeContext);
        String content = replacePlaceholders(definition.getContent(), safeContext);
        List<String> to = definition.getTo().stream().map(value -> replacePlaceholders(value, safeContext)).toList();
        List<String> cc = definition.getCc().stream().map(value -> replacePlaceholders(value, safeContext)).toList();
        String endpoint = replacePlaceholders(definition.getEndpoint(), safeContext);

        RemoteArtifact artifact = definition.getType() == TemplateType.REMOTE
                ? buildRemoteArtifact(definition, endpoint)
                : RemoteArtifact.empty();

        return new RenderedTemplate(subject, content, to, cc, endpoint,
                artifact.fileName(), artifact.content(), artifact.contentType(), artifact.metadata());
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

    private RemoteArtifact buildRemoteArtifact(TemplateDefinition definition, String endpoint) {
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
                String fileName = definition.getName().replaceAll("\\s+", "-").toLowerCase(Locale.ROOT) + ".rdp";
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
                    "error", "Failed to parse endpoint"));
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
