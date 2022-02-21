package io.smallrye.stork.loadbalancer.random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("random")
@LoadBalancerAttribute(name = "use-secure-random", defaultValue = "false", description = "Whether the load balancer should use a SecureRandom instead of a Random (default). Check [this page](https://stackoverflow.com/questions/11051205/difference-between-java-util-random-and-java-security-securerandom) to understand the difference")
public class RandomLoadBalancerProvider
        implements LoadBalancerProvider<RandomLoadBalancerProviderConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(RandomLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new RandomLoadBalancer(Boolean.parseBoolean(config.getUseSecureRandom()));
    }
}
