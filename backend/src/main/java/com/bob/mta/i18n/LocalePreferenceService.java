package com.bob.mta.i18n;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class LocalePreferenceService {

    private final LocaleSettingsRepository repository;
    private final List<Locale> supportedLocales;

    public LocalePreferenceService(LocaleSettingsRepository repository) {
        this.repository = repository;
        this.supportedLocales = List.of(Localization.getDefaultLocale(), Locale.CHINA);
    }

    public Locale getSystemDefaultLocale() {
        String stored = repository.getDefaultLocale();
        if (!StringUtils.hasText(stored)) {
            return Localization.getDefaultLocale();
        }
        return Locale.forLanguageTag(stored);
    }

    public List<Locale> getSupportedLocales() {
        return List.copyOf(supportedLocales);
    }

    public Locale resolveLocale(String acceptLanguageHeader) {
        Locale defaultLocale = getSystemDefaultLocale();
        if (!StringUtils.hasText(acceptLanguageHeader)) {
            return defaultLocale;
        }
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguageHeader);
            for (Locale.LanguageRange range : ranges) {
                Locale match = matchSupportedLocale(range.getRange());
                if (match != null) {
                    return match;
                }
            }
        } catch (IllegalArgumentException ignored) {
            Locale directMatch = matchSupportedLocale(acceptLanguageHeader);
            if (directMatch != null) {
                return directMatch;
            }
        }
        return defaultLocale;
    }

    public Locale updateDefaultLocale(String localeTag) {
        if (!StringUtils.hasText(localeTag)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    Localization.text(LocalizationKeys.Validation.MULTILINGUAL_LOCALE_REQUIRED));
        }
        Locale requested = Locale.forLanguageTag(localeTag);
        Locale matched = supportedLocales.stream()
                .filter(locale -> locale.toLanguageTag().equalsIgnoreCase(requested.toLanguageTag()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        Localization.text(LocalizationKeys.Errors.LOCALE_UNSUPPORTED, localeTag)));
        repository.updateDefaultLocale(matched.toLanguageTag());
        return matched;
    }

    private Locale matchSupportedLocale(String languageRange) {
        if (!StringUtils.hasText(languageRange)) {
            return null;
        }
        Locale candidate = Locale.forLanguageTag(languageRange);
        for (Locale locale : supportedLocales) {
            if (locale.getLanguage().equalsIgnoreCase(candidate.getLanguage())) {
                return locale;
            }
        }
        return null;
    }
}
