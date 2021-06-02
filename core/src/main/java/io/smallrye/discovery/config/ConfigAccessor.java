package io.smallrye.discovery.config;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigAccessor {

    private final Config config;

    public ConfigAccessor() {
        this(ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader()));
    }

    public ConfigAccessor(Config config) {
        this.config = config;
    }

    public List<String> getKeys() {
        List<String> keys = new LinkedList<>();

        for (String key : config.getPropertyNames()) {
            keys.add(key);
        }

        return keys;
    }

    public List<String> getKeys(String prefix) {
        List<String> keys = new LinkedList<>();

        for (String key : config.getPropertyNames()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }

        return keys;
    }

    public String getValue(String key) {
        return getValue(key, String.class);
    }

    public <T> T getValue(String key, Class<T> type) {
        return config.getValue(key, type);
    }
}
