package io.smallrye.stork.test;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("empty")
public class EmptyLoadBalancerProvider implements LoadBalancerProvider<EmptyLoadBalancerProviderConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(EmptyLoadBalancerProviderConfiguration config, ServiceDiscovery serviceDiscovery) {
        return null;
    }
}
