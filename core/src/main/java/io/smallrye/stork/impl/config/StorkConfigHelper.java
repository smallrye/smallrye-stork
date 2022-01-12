package io.smallrye.stork.impl.config;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.smallrye.stork.api.config.ServiceDiscoveryConfig;

public class StorkConfigHelper {

    private StorkConfigHelper() {
        // Avoid direct instantiation
    }

    public static Optional<String> get(ServiceDiscoveryConfig config, String name) {
        return Optional.ofNullable(config.parameters().get(name));
    }

    public static String getOrDefault(ServiceDiscoveryConfig config, String name, String def) {
        String val = config.parameters().get(name);
        if (val == null) {
            return def;
        }
        return val;
    }

    public static int getIntegerOrDefault(String sn, ServiceDiscoveryConfig config, String name, int def) {
        String v = config.parameters().get(name);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse the property `" + name
                    + "` to int from the service discovery configuration for service '" + sn + "'", e);
        }
    }

    public static Integer getInteger(String sn, String propertyName, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse the property `" + propertyName
                    + "` to int from the service discovery configuration for service '" + sn + "'", e);
        }
    }

    public static boolean getBooleanOrDefault(ServiceDiscoveryConfig config, String name, boolean def) {
        String v = config.parameters().get(name);
        if (v == null) {
            return def;
        }
        return Boolean.parseBoolean(v);

    }

    public static String getOrDie(String sn, ServiceDiscoveryConfig config, String name) {
        String val = config.parameters().get(name);
        if (val == null) {
            throw new NoSuchElementException(
                    "`" + name + "` must be set in the service discovery configuration for service '" + sn + "'");
        }
        return val;
    }
}
