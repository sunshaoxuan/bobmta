package com.bob.mta.common.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MultilingualTextPayload {

    private final String defaultLocale;
    private final Map<String, String> translations;

    @JsonCreator
    public MultilingualTextPayload(@JsonProperty("defaultLocale") String defaultLocale,
                                   @JsonProperty("translations") Map<String, String> translations) {
        this.defaultLocale = defaultLocale;
        this.translations = translations == null ? Collections.emptyMap() : new HashMap<>(translations);
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public Map<String, String> getTranslations() {
        return Collections.unmodifiableMap(translations);
    }

    public MultilingualText toValue() {
        return MultilingualText.of(defaultLocale, translations);
    }

    public static MultilingualTextPayload fromValue(MultilingualText text) {
        return fromValue(text, null);
    }

    public static MultilingualTextPayload fromValue(MultilingualText text, Locale locale) {
        if (text == null) {
            return null;
        }
        String requestedLocale = locale == null
                ? text.getDefaultLocale()
                : locale.toLanguageTag().toLowerCase(Locale.ROOT);
        Map<String, String> normalized = new HashMap<>(text.getTranslations());
        normalized.putIfAbsent(requestedLocale, text.getValueOrDefault(requestedLocale));
        return new MultilingualTextPayload(requestedLocale, normalized);
    }
}
