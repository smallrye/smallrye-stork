package io.smallrye.stork.test;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;

public class TestLoadBalancer1 implements LoadBalancer {

    private final TestLoadBalancer1ProviderConfiguration config;
    private final ServiceDiscovery serviceDiscovery;
    private final String type;

    public TestLoadBalancer1(TestLoadBalancer1ProviderConfiguration config, ServiceDiscovery serviceDiscovery, String type) {
        this.config = config;
        this.serviceDiscovery = serviceDiscovery;
        this.type = type;
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        return null;
    }

    public TestLoadBalancer1ProviderConfiguration getConfig() {
        return config;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public String getType() {
        return type;
    }
}
