package io.smallrye.stork.spi;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;

/**
 * A load balancer provider allowing to create instances of load balancers.
 * <p>
 * Implementation should use the {@link io.smallrye.stork.api.config.LoadBalancerAttribute} to define attributes.
 *
 * @param <T> the configuration type (class generated from the {@link io.smallrye.stork.api.config.LoadBalancerAttribute}
 *        annotations).
 */
public interface LoadBalancerProvider<T> {
    /**
     * Creates a load balancer instance
     *
     * @param config the configuration
     * @param serviceDiscovery the service discovery used for that service
     * @return the load balancer
     */
    LoadBalancer createLoadBalancer(T config, ServiceDiscovery serviceDiscovery);
}
