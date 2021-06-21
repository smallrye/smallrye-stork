package io.smallrye.dux.loadbalancer.roundrobin;

import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.dux.spi.LoadBalancerProvider;

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
