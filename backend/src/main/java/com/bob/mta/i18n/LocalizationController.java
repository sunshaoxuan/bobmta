package com.bob.mta.i18n;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.i18n.dto.LocaleSettingsResponse;
import com.bob.mta.i18n.dto.LocalizationBundleResponse;
import com.bob.mta.i18n.dto.UpdateDefaultLocaleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/i18n")
public class LocalizationController {

    private final LocalePreferenceService localePreferenceService;

    public LocalizationController(LocalePreferenceService localePreferenceService) {
        this.localePreferenceService = localePreferenceService;
    }

    @GetMapping("/messages")
    public ApiResponse<LocalizationBundleResponse> messages(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Locale resolvedLocale = localePreferenceService.resolveLocale(acceptLanguage);
        Locale defaultLocale = localePreferenceService.getSystemDefaultLocale();
        Map<String, String> localizedMessages = Localization.bundle(resolvedLocale);
        Map<String, String> defaultMessages = Localization.bundle(defaultLocale);
        String version = defaultMessages.getOrDefault(LocalizationKeys.Frontend.BUNDLE_VERSION, "1");
        List<String> supportedLocales = localePreferenceService.getSupportedLocales().stream()
                .map(Locale::toLanguageTag)
                .toList();
        LocalizationBundleResponse response = new LocalizationBundleResponse(
                resolvedLocale.toLanguageTag(),
                defaultLocale.toLanguageTag(),
                version,
                supportedLocales,
                localizedMessages,
                defaultMessages
        );
        return ApiResponse.success(response);
    }

    @PutMapping("/default-locale")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LocaleSettingsResponse> updateDefaultLocale(
            @Valid @RequestBody UpdateDefaultLocaleRequest request) {
        Locale updatedLocale = localePreferenceService.updateDefaultLocale(request.locale());
        List<String> supported = localePreferenceService.getSupportedLocales().stream()
                .map(Locale::toLanguageTag)
                .toList();
        LocaleSettingsResponse response = new LocaleSettingsResponse(updatedLocale.toLanguageTag(), supported);
        return ApiResponse.success(response);
    }
}
