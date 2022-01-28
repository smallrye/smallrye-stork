package io.smallrye.stork.loadbalancer.random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("random")
public class RandomLoadBalancerProvider
        implements LoadBalancerProvider<RandomLoadBalancerProviderConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(RandomLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new RandomLoadBalancer();
    }
}
