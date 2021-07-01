package io.smallrye.stork.test;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.LoadBalancerConfig;

public class TestLoadBalancer implements LoadBalancer {

    private final LoadBalancerConfig config;
    private final ServiceDiscovery serviceDiscovery;
    private final String type;

    public TestLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery, String type) {
        this.config = config;
        this.serviceDiscovery = serviceDiscovery;
        this.type = type;
    }

    @Override
    public Uni<ServiceInstance> selectServiceInstance() {
        return null;
    }

    @Override
    public ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances) {
        return null;
    }

    public LoadBalancerConfig getConfig() {
        return config;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public String getType() {
        return type;
    }
}
