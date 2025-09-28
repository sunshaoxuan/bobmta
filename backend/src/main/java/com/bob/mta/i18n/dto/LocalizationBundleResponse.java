package com.bob.mta.i18n.dto;

import java.util.List;
import java.util.Map;

public record LocalizationBundleResponse(
        String locale,
        String defaultLocale,
        String version,
        List<String> supportedLocales,
        Map<String, String> messages,
        Map<String, String> defaultMessages
) {
}
