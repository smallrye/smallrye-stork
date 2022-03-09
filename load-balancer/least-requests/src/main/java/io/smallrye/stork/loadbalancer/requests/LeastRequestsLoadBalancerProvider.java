package io.smallrye.stork.loadbalancer.requests;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("least-requests")
public class LeastRequestsLoadBalancerProvider
        implements LoadBalancerProvider<LeastRequestsConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(LeastRequestsConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new LeastRequestsLoadBalancer();
    }
}
