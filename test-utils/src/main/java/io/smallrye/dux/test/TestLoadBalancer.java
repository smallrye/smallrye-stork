package io.smallrye.dux.test;

import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.mutiny.Uni;

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
