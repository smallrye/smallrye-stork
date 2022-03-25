package io.smallrye.stork.test;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestServiceDiscovery implements ServiceDiscovery {

    private final TestSd1Configuration config;
    private final String type;

    public TestServiceDiscovery(TestSd1Configuration config, String type) {
        this.config = config;
        this.type = type;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return null;
    }

    public TestSd1Configuration getConfig() {
        return config;
    }

    public String getType() {
        return type;
    }
}
