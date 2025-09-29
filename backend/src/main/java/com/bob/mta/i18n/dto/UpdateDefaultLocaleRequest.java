package com.bob.mta.i18n.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDefaultLocaleRequest(
        @NotBlank(message = "{validation.multilingual.localeRequired}")
        String locale
) {
}
