package io.smallrye.dux.test;

import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.dux.spi.LoadBalancerProvider;

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
