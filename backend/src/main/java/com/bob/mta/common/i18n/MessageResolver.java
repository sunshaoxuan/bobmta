package com.bob.mta.common.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null) {
            locale = Locale.SIMPLIFIED_CHINESE;
        }
        return messageSource.getMessage(code, args, code, locale);
    }
}
