package com.bob.mta.i18n;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Localization {

    private static final String BASE_NAME = "i18n.messages";
    private static final Locale DEFAULT_LOCALE = Locale.JAPAN;
    private static final ConcurrentMap<Locale, ResourceBundle> CACHE = new ConcurrentHashMap<>();

    private Localization() {
    }

    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

    public static String text(String code, Object... args) {
        return text(DEFAULT_LOCALE, code, args);
    }

    public static String text(Locale locale, String code, Object... args) {
        ResourceBundle bundle = CACHE.computeIfAbsent(locale, key -> ResourceBundle.getBundle(BASE_NAME, key));
        String pattern = bundle.getString(code);
        if (args == null || args.length == 0) {
            return pattern;
        }
        MessageFormat format = new MessageFormat(pattern, locale);
        return format.format(args);
    }

    public static Map<String, String> bundle(Locale locale) {
        ResourceBundle bundle = CACHE.computeIfAbsent(locale, key -> ResourceBundle.getBundle(BASE_NAME, key));
        Map<String, String> messages = new HashMap<>();
        for (String key : bundle.keySet()) {
            messages.put(key, bundle.getString(key));
        }
        return Map.copyOf(messages);
    }
}
