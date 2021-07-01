package io.smallrye.stork.loadbalancer.roundrobin;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.spi.LoadBalancerProvider;

public class RoundRobinLoadBalancerProvider implements LoadBalancerProvider {

    @Override
    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
        return new RoundRobinLoadBalancer(serviceDiscovery);
    }

    @Override
    public String type() {
        return "round-robin";
    }
}
