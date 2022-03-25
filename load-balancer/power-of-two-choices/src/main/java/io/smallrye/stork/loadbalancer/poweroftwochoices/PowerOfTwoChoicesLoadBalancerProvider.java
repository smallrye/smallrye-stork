package io.smallrye.stork.loadbalancer.poweroftwochoices;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("power-of-two-choices")
@LoadBalancerAttribute(name = "use-secure-random", defaultValue = "false", description = "Whether the load balancer should use a SecureRandom instead of a Random (default). Check [this page](https://stackoverflow.com/questions/11051205/difference-between-java-util-random-and-java-security-securerandom) to understand the difference")
public class PowerOfTwoChoicesLoadBalancerProvider
        implements LoadBalancerProvider<PowerOfTwoChoicesConfiguration> {

    public LoadBalancer createLoadBalancer(PowerOfTwoChoicesConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new PowerOfTwoChoicesLoadBalancer(Boolean.parseBoolean(config.getUseSecureRandom()));
    }
}
