package io.smallrye.stork;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;

public class FakeServiceConfig implements ServiceConfig {

    private final String name;
    private final ConfigWithType lb;
    private final ConfigWithType sd;

    public FakeServiceConfig(String name, ConfigWithType sd, ConfigWithType lb) {
        this.name = name;
        this.lb = lb;
        this.sd = sd;
    }

    @Override
    public String serviceName() {
        return name;
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
    public boolean secure() {
        return false;
    }
}
