package com.kremecn.geyser.extension.betterpack.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LanguageManager {

    private static final Map<String, Properties> locales = new HashMap<>();
    private static final String DEFAULT_LOCALE = "en_US";

    public static void init() {
        loadLocale("en_US");
        loadLocale("zh_CN");
        loadLocale("zh_TW");
    }

    private static void loadLocale(String locale) {
        Properties properties = new Properties();
        try (InputStream stream = LanguageManager.class.getClassLoader()
                .getResourceAsStream("languages/" + locale + ".properties")) {
            if (stream != null) {
                properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                locales.put(locale, properties);
            } else {
                System.err.println("Could not find locale file for: " + locale);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String locale, String key) {
        Properties props = locales.get(locale);
        if (props == null || !props.containsKey(key)) {
            props = locales.get(DEFAULT_LOCALE);
        }
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }
        return key;
    }
}
