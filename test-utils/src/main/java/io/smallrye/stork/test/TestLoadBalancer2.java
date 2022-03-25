package io.smallrye.stork.test;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestLoadBalancer2 implements LoadBalancer {

    private final TestLb2Configuration config;
    private final ServiceDiscovery serviceDiscovery;
    private final String type;

    public TestLoadBalancer2(TestLb2Configuration config, ServiceDiscovery serviceDiscovery, String type) {
        this.config = config;
        this.serviceDiscovery = serviceDiscovery;
        this.type = type;
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        return null;
    }

    public TestLb2Configuration getConfig() {
        return config;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public String getType() {
        return type;
    }
}
