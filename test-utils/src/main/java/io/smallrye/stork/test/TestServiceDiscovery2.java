package io.smallrye.stork.test;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestServiceDiscovery2 implements ServiceDiscovery {

    private final TestSd2Configuration config;
    private final String type;

    public TestServiceDiscovery2(TestSd2Configuration config, String type, boolean secure) {
        this.config = config;
        this.type = type;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return null;
    }

    public TestSd2Configuration getConfig() {
        return config;
    }

    public String getType() {
        return type;
    }
}
