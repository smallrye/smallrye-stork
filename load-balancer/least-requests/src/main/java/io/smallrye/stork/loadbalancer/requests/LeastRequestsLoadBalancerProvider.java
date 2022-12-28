package io.smallrye.stork.loadbalancer.requests;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * A load balancer provider that picks the instance with the less inflight requests.
 */
@LoadBalancerType("least-requests")
@ApplicationScoped
public class LeastRequestsLoadBalancerProvider
        implements LoadBalancerProvider<LeastRequestsConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(LeastRequestsConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new LeastRequestsLoadBalancer();
    }
}
