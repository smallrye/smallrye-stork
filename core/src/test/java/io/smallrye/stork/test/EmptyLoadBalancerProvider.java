package io.smallrye.stork.test;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("empty-selector")
public class EmptyLoadBalancerProvider implements LoadBalancerProvider<EmptySelectorConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(EmptySelectorConfiguration config, ServiceDiscovery serviceDiscovery) {
        return null;
    }
}
