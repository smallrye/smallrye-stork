package io.smallrye.stork.loadbalancer.leastresponsetime;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("least-response-time")
@LoadBalancerAttribute(name = "force-retry-threshold", defaultValue = "1000", description = "after how many calls should a service instance reuse be forced on no failures")
@LoadBalancerAttribute(name = "retry-after-failure-threshold", defaultValue = "10000", description = "after how many calls should a service instance reuse be forced after failure")
public class LeastResponseTimeLoadBalancerProvider
        implements LoadBalancerProvider<LeastResponseTimeLoadBalancerProviderConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(LeastResponseTimeLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new LeastResponseTimeLoadBalancer(config);
    }
}
