package io.smallrye.dux.test;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.config.ServiceDiscoveryConfig;
import io.smallrye.mutiny.Multi;

public class TestServiceDiscovery implements ServiceDiscovery {

    private final ServiceDiscoveryConfig config;
    private final String type;

    public TestServiceDiscovery(ServiceDiscoveryConfig config, String type) {
        this.config = config;
        this.type = type;
    }

    @Override
    public Multi<ServiceInstance> getServiceInstances() {
        return null;
    }

    public ServiceDiscoveryConfig getConfig() {
        return config;
    }

    public String getType() {
        return type;
    }
}
