package io.smallrye.stork.spi.internal;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.spi.ElementWithType;

/**
 * Used by stork internals to generate service loader for LoadBalancerProvider
 */
public interface LoadBalancerLoader extends ElementWithType {
    LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery);
}
