package com.bob.mta.i18n;

public interface LocaleSettingsRepository {

    String getDefaultLocale();

    void updateDefaultLocale(String locale);
}
