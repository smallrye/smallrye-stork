package io.smallrye.stork.impl;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.Stork;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * Round-robin is the only implementation built-in in the Stork API.
 * It is used when no load-balancer configuration is given.
 *
 * Note that it is not registered using the SPI, but directly in {@link Stork#initialize()}.
 */
public class RoundRobinLoadBalancerProvider implements LoadBalancerProvider {

    public static final String ROUND_ROBIN_TYPE = "round-robin";

    @Override
    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
        return new RoundRobinLoadBalancer();
    }

    @Override
    public String type() {
        return ROUND_ROBIN_TYPE;
    }
}
