package io.smallrye.stork.impl;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * Round-robin is the only implementation built-in in the Stork API.
 * It is used when no load-balancer configuration is given.
 *
 * Note that it is not registered using the SPI, but directly in {@link Stork#initialize()}.
 */
@LoadBalancerType(RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE)
public class RoundRobinLoadBalancerProvider
        implements LoadBalancerProvider<io.smallrye.stork.impl.RoundRobinLoadBalancerProviderConfiguration> {

    public static final String ROUND_ROBIN_TYPE = "round-robin";

    @Override
    public LoadBalancer createLoadBalancer(io.smallrye.stork.impl.RoundRobinLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new RoundRobinLoadBalancer();
    }
}
