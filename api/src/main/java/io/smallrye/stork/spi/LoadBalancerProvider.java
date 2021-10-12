package io.smallrye.stork.spi;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.LoadBalancerConfig;

public interface LoadBalancerProvider extends ElementWithType {
    LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery);
}
