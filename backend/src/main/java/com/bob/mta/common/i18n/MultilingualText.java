package com.bob.mta.common.i18n;

import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;

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
            throw new IllegalArgumentException(
                    Localization.text(LocalizationKeys.Validation.MULTILINGUAL_DEFAULT_LOCALE_REQUIRED));
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
            throw new IllegalArgumentException(
                    Localization.text(LocalizationKeys.Validation.MULTILINGUAL_DEFAULT_VALUE_REQUIRED));
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
            throw new IllegalArgumentException(
                    Localization.text(LocalizationKeys.Validation.MULTILINGUAL_LOCALE_REQUIRED));
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
            throw new IllegalArgumentException(
                    Localization.text(LocalizationKeys.Validation.MULTILINGUAL_MERGE_DEFAULT_MISSING));
        }
        return new MultilingualText(effectiveDefault, merged);
    }
}
