package com.bob.mta.i18n.dto;

import java.util.List;

public record LocaleSettingsResponse(String defaultLocale, List<String> supportedLocales) {
}
