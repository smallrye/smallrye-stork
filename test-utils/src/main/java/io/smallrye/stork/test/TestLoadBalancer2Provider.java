package io.smallrye.stork.test;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType(TestLoadBalancer2Provider.TYPE)
@LoadBalancerAttribute(name = "some-prop", description = "no description")
public class TestLoadBalancer2Provider implements LoadBalancerProvider<TestLb2Configuration> {

    public static final String TYPE = "test-lb-2";

    @Override
    public LoadBalancer createLoadBalancer(TestLb2Configuration config, ServiceDiscovery serviceDiscovery) {
        return new TestLoadBalancer2(config, serviceDiscovery, TYPE);
    }

}
