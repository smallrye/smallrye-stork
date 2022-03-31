package io.smallrye.stork.loadbalancer.random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * A load balancer provider that choose a random service instance from the discovered list of service instances.
 */
@LoadBalancerType("random")
@LoadBalancerAttribute(name = "use-secure-random", defaultValue = "false", description = "Whether the load balancer should use a SecureRandom instead of a Random (default). Check [this page](https://stackoverflow.com/questions/11051205/difference-between-java-util-random-and-java-security-securerandom) to understand the difference")
public class RandomLoadBalancerProvider
        implements LoadBalancerProvider<RandomConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(RandomConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new RandomLoadBalancer(Boolean.parseBoolean(config.getUseSecureRandom()));
    }
}
