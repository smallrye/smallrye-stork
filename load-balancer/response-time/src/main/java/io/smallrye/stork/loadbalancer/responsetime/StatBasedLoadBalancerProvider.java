package io.smallrye.stork.loadbalancer.responsetime;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.spi.LoadBalancerProvider;

public class StatBasedLoadBalancerProvider implements LoadBalancerProvider {

    @Override
    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
        return new StatBasedLoadBalancer();
    }

    @Override
    public String type() {
        return "stat-based";
    }
}
