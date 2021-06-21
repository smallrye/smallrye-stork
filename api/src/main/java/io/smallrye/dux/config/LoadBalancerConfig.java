package io.smallrye.dux.config;

import java.util.Map;

public interface LoadBalancerConfig {
    String type();

    Map<String, String> parameters();
}
