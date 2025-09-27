package com.bob.mta.i18n;

import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Repository
public class InMemoryLocaleSettingsRepository implements LocaleSettingsRepository {

    private final AtomicReference<String> defaultLocale =
            new AtomicReference<>(Localization.getDefaultLocale().toLanguageTag());

    @Override
    public String getDefaultLocale() {
        return defaultLocale.get();
    }

    @Override
    public void updateDefaultLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return;
        }
        defaultLocale.set(Locale.forLanguageTag(locale).toLanguageTag());
    }
}
