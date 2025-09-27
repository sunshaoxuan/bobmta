package com.bob.mta.common.i18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MultilingualText {

    private final String defaultLocale;
    private final Map<String, String> translations;

    private MultilingualText(String defaultLocale, Map<String, String> translations) {
        if (defaultLocale == null || defaultLocale.isBlank()) {
            throw new IllegalArgumentException("defaultLocale is required");
        }
        this.defaultLocale = normalize(defaultLocale);
        Map<String, String> normalized = new HashMap<>();
        if (translations != null) {
            translations.forEach((locale, text) -> {
                if (text != null) {
                    normalized.put(normalize(locale), text);
                }
            });
        }
        if (!normalized.containsKey(this.defaultLocale)) {
            throw new IllegalArgumentException("translations must contain default locale value");
        }
        this.translations = Collections.unmodifiableMap(normalized);
    }

    public static MultilingualText of(String defaultLocale, Map<String, String> translations) {
        return new MultilingualText(defaultLocale, translations);
    }

    public static MultilingualText single(String locale, String text) {
        Map<String, String> translations = new HashMap<>();
        translations.put(normalize(locale), Objects.requireNonNull(text));
        return new MultilingualText(locale, translations);
    }

    private static String normalize(String locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale is required");
        }
        return locale.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public String getValueOrDefault(String locale) {
        if (locale == null || locale.isBlank()) {
            return translations.get(defaultLocale);
        }
        String normalized = normalize(locale);
        return translations.getOrDefault(normalized, translations.get(defaultLocale));
    }

    public MultilingualText merge(Map<String, String> overrides, String newDefaultLocale) {
        Map<String, String> merged = new HashMap<>(translations);
        if (overrides != null) {
            overrides.forEach((locale, text) -> {
                if (text != null) {
                    merged.put(normalize(locale), text);
                }
            });
        }
        String effectiveDefault = newDefaultLocale == null || newDefaultLocale.isBlank()
                ? defaultLocale : normalize(newDefaultLocale);
        if (!merged.containsKey(effectiveDefault)) {
            throw new IllegalArgumentException("default locale value missing after merge");
        }
        return new MultilingualText(effectiveDefault, merged);
    }
}
