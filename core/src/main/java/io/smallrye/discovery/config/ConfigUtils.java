package io.smallrye.discovery.config;

public final class ConfigUtils {

    public static String keySegment(String key, int position) {
        if (key == null) {
            return null;
        }

        String[] segments = key.split("\\.");
        if (segments.length > position) {
            return segments[position];
        }

        return null;
    }

    public static String keyWithoutPrefix(String key, String prefix) {
        if (key == null) {
            return null;
        }

        if (key.startsWith(prefix)) {
            return key.replaceFirst(prefix, "");
        }

        return key;
    }
}
