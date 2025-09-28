package com.bob.mta.i18n;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.i18n.dto.LocalizationBundleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizationControllerTest {

    private LocalizationController controller;

    @BeforeEach
    void setUp() {
        controller = new LocalizationController(new LocalePreferenceService(new InMemoryLocaleSettingsRepository()));
    }

    @Test
    void shouldReturnLocalizedBundleForRequestedLocale() {
        ApiResponse<LocalizationBundleResponse> response = controller.messages("zh-CN");
        LocalizationBundleResponse data = response.getData();

        assertThat(data.locale()).isEqualTo("zh-CN");
        assertThat(data.defaultLocale()).isEqualTo("ja-JP");
        assertThat(data.supportedLocales()).contains("ja-JP", "zh-CN");
        assertThat(data.messages()).containsKey(LocalizationKeys.Frontend.APP_TITLE);
        assertThat(data.defaultMessages()).containsKey(LocalizationKeys.Frontend.BUNDLE_VERSION);
        assertThat(data.defaultMessages().get(LocalizationKeys.Frontend.BUNDLE_VERSION)).isNotBlank();
    }

    @Test
    void shouldFallbackToDefaultForUnsupportedLocale() {
        ApiResponse<LocalizationBundleResponse> response = controller.messages("fr-FR");
        LocalizationBundleResponse data = response.getData();

        assertThat(data.locale()).isEqualTo("ja-JP");
        assertThat(data.messages()).containsKey(LocalizationKeys.Frontend.APP_TITLE);
    }
}
