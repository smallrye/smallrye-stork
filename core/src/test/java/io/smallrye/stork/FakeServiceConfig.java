package io.smallrye.stork;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;

public class FakeServiceConfig implements ServiceConfig {

    private final String name;
    private final ConfigWithType lb;
    private final ConfigWithType sd;
    private final ConfigWithType sr;

    public FakeServiceConfig(String name, ConfigWithType sd, ConfigWithType lb, ConfigWithType sr) {
        this.name = name;
        this.lb = lb;
        this.sd = sd;
        this.sr = sr;
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
    public ConfigWithType serviceRegistrar() {
        return sr;
    }

    @Override
    public boolean secure() {
        return false;
    }
}
