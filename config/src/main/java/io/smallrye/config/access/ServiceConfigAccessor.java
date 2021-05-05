package io.smallrye.config.access;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ServiceConfigAccessor {

    private final Config config;

    private final String prefix;

    public ServiceConfigAccessor(String prefix) {
        this(ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader()), prefix);
    }

    public ServiceConfigAccessor(Config config, String prefix) {
        this.config = config;
        this.prefix = prefix;
    }

    public String getValue(String serviceName, String key) {
        return getValue(serviceName, key, String.class);
    }

    public <T> T getValue(String serviceName, String key, Class<T> type) {
        String fullKey = String.format("%s.%s.%s", prefix, serviceName, key);

        return config.getValue(fullKey, type);
    }

    public List<String> getKeys(String serviceName) {
        String servicePrefix = String.format("%s.%s.", prefix, serviceName);

        return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(name -> name.startsWith(servicePrefix))
                .map(name -> name.replace(servicePrefix, ""))
                .collect(Collectors.toList());
    }
}
