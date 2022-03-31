package io.smallrye.stork.loadbalancer.leastresponsetime;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

/**
 * A load balancer provider following response times and failures.
 */
@LoadBalancerType("least-response-time")
@LoadBalancerAttribute(name = "declining-factor", defaultValue = "0.9", description = "How much *score* should decline in time, see Score calculation in the docs for details.")
@LoadBalancerAttribute(name = "error-penalty", defaultValue = "60s", description = "This load balancer treats an erroneous response as a response after this time.")
@LoadBalancerAttribute(name = "use-secure-random", defaultValue = "false", description = "Whether the load balancer should use a SecureRandom instead of a Random (default). Check [this page](https://stackoverflow.com/questions/11051205/difference-between-java-util-random-and-java-security-securerandom) to understand the difference")
public class LeastResponseTimeLoadBalancerProvider
        implements LoadBalancerProvider<LeastResponseTimeConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(LeastResponseTimeConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new LeastResponseTimeLoadBalancer(config);
    }
}
