package io.smallrye.stork.test;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestServiceDiscovery implements ServiceDiscovery {

    private final TestServiceDiscovery1ProviderConfiguration config;
    private final String type;

    public TestServiceDiscovery(TestServiceDiscovery1ProviderConfiguration config, String type, boolean secure) {
        this.config = config;
        this.type = type;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return null;
    }

    public TestServiceDiscovery1ProviderConfiguration getConfig() {
        return config;
    }

    public String getType() {
        return type;
    }
}
