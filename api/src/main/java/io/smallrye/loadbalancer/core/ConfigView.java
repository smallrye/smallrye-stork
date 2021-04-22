package io.smallrye.loadbalancer.core;

import java.util.Iterator;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

// TODO: probably should be moved to some implementation module, not needed for "clients"
public class ConfigView implements Config {
    private final Config rootConfig;
    private final String prefix;

    public ConfigView(Config rootConfig, String name, String prefix) {
        this.rootConfig = rootConfig;
        this.prefix = String.format("%s.%s", prefix, name);
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return rootConfig.getValue(rootKey(propertyName), propertyType);
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return rootConfig.getConfigValue(rootKey(propertyName));
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return rootConfig.getOptionalValue(rootKey(propertyName), propertyType);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Iterator<String> rootIterator = rootConfig.getPropertyNames().iterator();

        return () -> new Iterator<String>() {
            String next = getNext();

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public String next() {
                String result = next;
                next = getNext();
                return result;
            }

            private String getNext() {
                while (rootIterator.hasNext()) {
                    if (matchesPrefix(next = rootIterator.next())) {
                        return next;
                    }
                }
                return null;
            }

            private boolean matchesPrefix(String propertyName) {
                return propertyName.startsWith(prefix);
            }
        };

    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return rootConfig.getConfigSources();
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return rootConfig.getConverter(forType);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new IllegalArgumentException("not implemented");
    }

    private String rootKey(String propertyName) {
        return String.format("%s.%s", prefix, propertyName);
    }
}
