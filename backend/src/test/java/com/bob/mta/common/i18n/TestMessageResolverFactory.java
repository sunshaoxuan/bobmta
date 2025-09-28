package com.bob.mta.common.i18n;

import java.nio.charset.StandardCharsets;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public final class TestMessageResolverFactory {

    private TestMessageResolverFactory() {
    }

    public static MessageResolver create() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return new MessageResolver(messageSource);
    }
}
