package io.smallrye.dux.spi;

import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.LoadBalancerConfig;

public interface LoadBalancerProvider extends ElementWithType {
    LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery);

    String type();
}
