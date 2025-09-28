package com.bob.mta.i18n;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.i18n.dto.LocaleSettingsResponse;
import com.bob.mta.i18n.dto.LocalizationBundleResponse;
import com.bob.mta.i18n.dto.UpdateDefaultLocaleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldUpdateDefaultLocaleWhenSupported() {
        ApiResponse<LocaleSettingsResponse> response =
                controller.updateDefaultLocale(new UpdateDefaultLocaleRequest("zh-CN"));

        LocaleSettingsResponse settings = response.getData();
        assertThat(settings.defaultLocale()).isEqualTo("zh-CN");
        assertThat(settings.supportedLocales()).contains("ja-JP", "zh-CN");
        assertThat(controller.messages("zh-CN").getData().defaultLocale()).isEqualTo("zh-CN");
    }

    @Test
    void shouldRejectUnsupportedLocaleUpdate() {
        assertThatThrownBy(() -> controller.updateDefaultLocale(new UpdateDefaultLocaleRequest("fr-FR")))
                .isInstanceOf(BusinessException.class);
    }
}
