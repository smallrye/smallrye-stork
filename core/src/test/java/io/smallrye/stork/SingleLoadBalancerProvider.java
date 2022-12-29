package io.smallrye.stork;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("single")

public class SingleLoadBalancerProvider implements LoadBalancerProvider<SingleConfiguration> {
    @Override
    public LoadBalancer createLoadBalancer(SingleConfiguration config, ServiceDiscovery serviceDiscovery) {
        return new SingleLoadBalancer();
    }
}
