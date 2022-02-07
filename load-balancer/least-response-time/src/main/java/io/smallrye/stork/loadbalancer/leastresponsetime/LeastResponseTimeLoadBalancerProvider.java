package io.smallrye.stork.loadbalancer.leastresponsetime;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("least-response-time")
@LoadBalancerAttribute(name = "declining-factor", defaultValue = "0.9", description = "How much *score* should decline in time, see Score calculation in the docs for details.")
@LoadBalancerAttribute(name = "error-penalty", defaultValue = "60s", description = "This load balancer treats an erroneous response as a response after this time.")
public class LeastResponseTimeLoadBalancerProvider
        implements LoadBalancerProvider<LeastResponseTimeLoadBalancerProviderConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(LeastResponseTimeLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new LeastResponseTimeLoadBalancer(config);
    }
}
