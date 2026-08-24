package com.sunrise.dentalclinic.pattern.singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GoF Singleton holding a small read-mostly cache of application-wide
 * runtime settings that plain-Java helper classes - which are not Spring
 * beans and therefore cannot use {@code @Autowired} - need to read without
 * being handed a reference explicitly. Populated once at startup by
 * {@code AppStartupInitializer}.
 */
public final class AppConfigManager {

    private static final AppConfigManager INSTANCE = new AppConfigManager();

    private final Map<String, String> settings = new ConcurrentHashMap<>();

    private AppConfigManager() {
    }

    public static AppConfigManager getInstance() {
        return INSTANCE;
    }

    public void set(String key, String value) {
        settings.put(key, value);
    }

    public String get(String key) {
        return settings.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}
