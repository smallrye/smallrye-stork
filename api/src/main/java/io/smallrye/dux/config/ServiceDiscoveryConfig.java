package io.smallrye.dux.config;

import java.util.Map;

public interface ServiceDiscoveryConfig {
    String type();

    Map<String, String> parameters();
}
