package io.smallrye.stork.test;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestLoadBalancer1 implements LoadBalancer {

    private final TestLb1Configuration config;
    private final ServiceDiscovery serviceDiscovery;
    private final String type;

    public TestLoadBalancer1(TestLb1Configuration config, ServiceDiscovery serviceDiscovery, String type) {
        this.config = config;
        this.serviceDiscovery = serviceDiscovery;
        this.type = type;
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        return null;
    }

    public TestLb1Configuration getConfig() {
        return config;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public String getType() {
        return type;
    }
}
