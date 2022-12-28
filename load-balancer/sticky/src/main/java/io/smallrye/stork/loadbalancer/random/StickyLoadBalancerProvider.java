package io.smallrye.stork.loadbalancer.random;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;
import io.smallrye.stork.utils.DurationUtils;

/**
 * A load balancer provider that keep returning the same instance until that instance becomes unavailable or fails.
 */
@LoadBalancerType(StickyLoadBalancerProvider.TYPE)
@LoadBalancerAttribute(name = StickyLoadBalancerProvider.FAILURE_BACKOFF_TIME, defaultValue = "0", description = "After how much time, "
        + "a service instance that has failed can be reused.")
@ApplicationScoped
public class StickyLoadBalancerProvider
        implements LoadBalancerProvider<StickyConfiguration> {

    static final String TYPE = "sticky";
    static final String FAILURE_BACKOFF_TIME = "failure-backoff-time";

    @Override
    public LoadBalancer createLoadBalancer(StickyConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return new StickyLoadBalancer(DurationUtils.parseDuration(config.getFailureBackoffTime(), FAILURE_BACKOFF_TIME));
    }
}
