package io.smallrye.stork;

import java.util.Collections;
import java.util.Map;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;

public class FakeServiceConfig implements ServiceConfig {

    private final String serviceName;
    private final ConfigWithType lb;
    private final ConfigWithType sd;
    private final ConfigWithType sr;

    public FakeServiceConfig(String name, ConfigWithType sd, ConfigWithType lb, ConfigWithType sr) {
        this.serviceName = name;
        this.lb = lb;
        this.sd = sd;
        this.sr = sr;
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public ConfigWithType loadBalancer() {
        return lb;
    }

    @Override
    public ConfigWithType serviceDiscovery() {
        return sd;
    }

    @Override
    public ConfigWithType serviceRegistrar() {
        return sr;
    }

    @Override
    public boolean secure() {
        return false;
    }

    public static final ConfigWithType FAKE_SERVICE_DISCOVERY_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    public static final ConfigWithType FAKE_SECURE_SERVICE_DISCOVERY_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("secure", "true");
        }
    };

    public static final ConfigWithType FAKE_LOAD_BALANCER_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake-selector";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };
}
