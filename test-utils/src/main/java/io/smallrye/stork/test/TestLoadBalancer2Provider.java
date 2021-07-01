package io.smallrye.stork.test;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.spi.LoadBalancerProvider;

public class TestLoadBalancer2Provider implements LoadBalancerProvider {

    public static final String TYPE = "test-lb-2";

    @Override
    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
        return new TestLoadBalancer(config, serviceDiscovery, TYPE);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
