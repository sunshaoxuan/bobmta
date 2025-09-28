package com.bob.mta.i18n;

import com.bob.mta.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalePreferenceServiceTest {

    private InMemoryLocaleSettingsRepository repository;
    private LocalePreferenceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLocaleSettingsRepository();
        service = new LocalePreferenceService(repository);
    }

    @Test
    void shouldUpdateDefaultLocaleWithSupportedValue() {
        service.updateDefaultLocale("zh-CN");

        assertThat(repository.getDefaultLocale()).isEqualTo("zh-CN");
        assertThat(service.getSystemDefaultLocale().toLanguageTag()).isEqualTo("zh-CN");
    }

    @Test
    void shouldRejectUnsupportedLocale() {
        assertThatThrownBy(() -> service.updateDefaultLocale("fr-FR"))
                .isInstanceOf(BusinessException.class);
    }
}
