package io.smallrye.stork.spi;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;

public interface LoadBalancerProvider<T> {
    LoadBalancer createLoadBalancer(T config, ServiceDiscovery serviceDiscovery);
}
