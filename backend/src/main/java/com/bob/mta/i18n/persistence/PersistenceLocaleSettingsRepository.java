package com.bob.mta.i18n.persistence;

import com.bob.mta.i18n.LocaleSettingsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;

@Repository
@ConditionalOnBean(LocaleSettingsMapper.class)
public class PersistenceLocaleSettingsRepository implements LocaleSettingsRepository {

    static final String DEFAULT_LOCALE_KEY = "default_locale";

    private final LocaleSettingsMapper mapper;

    public PersistenceLocaleSettingsRepository(LocaleSettingsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String getDefaultLocale() {
        LocaleSettingEntity entity = mapper.find(DEFAULT_LOCALE_KEY);
        return entity == null ? null : entity.value();
    }

    @Override
    public void updateDefaultLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return;
        }
        Locale normalized = Locale.forLanguageTag(locale);
        LocaleSettingEntity entity = new LocaleSettingEntity(
                DEFAULT_LOCALE_KEY,
                normalized.toLanguageTag(),
                OffsetDateTime.now()
        );
        int updated = mapper.update(entity);
        if (updated == 0) {
            mapper.insert(entity);
        }
    }
}

