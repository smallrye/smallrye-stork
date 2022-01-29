package io.smallrye.stork.loadbalancer.poweroftwochoices;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("power-of-two-choices")
public class PowerOfTwoChoicesLoadBalancerProvider
        implements LoadBalancerProvider<PowerOfTwoChoicesLoadBalancerProviderConfiguration> {

    public LoadBalancer createLoadBalancer(PowerOfTwoChoicesLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new PowerOfTwoChoicesLoadBalancer();
    }
}
