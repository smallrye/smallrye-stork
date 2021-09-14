package io.smallrye.stork.loadbalancer.leastresponsetime;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.spi.LoadBalancerProvider;

public class LeastResponseTimeLoadBalancerProvider implements LoadBalancerProvider {

    @Override
    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
        return new LeastResponseTimeLoadBalancer();
    }

    @Override
    public String type() {
        return "least-response-time";
    }
}
