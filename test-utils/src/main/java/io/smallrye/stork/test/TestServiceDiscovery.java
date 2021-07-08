package io.smallrye.stork.test;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;

public class TestServiceDiscovery implements ServiceDiscovery {

    private final ServiceDiscoveryConfig config;
    private final String type;

    public TestServiceDiscovery(ServiceDiscoveryConfig config, String type) {
        this.config = config;
        this.type = type;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return null;
    }

    public ServiceDiscoveryConfig getConfig() {
        return config;
    }

    public String getType() {
        return type;
    }
}
