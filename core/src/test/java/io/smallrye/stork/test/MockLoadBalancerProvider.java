package io.smallrye.stork.test;

import static org.mockito.Mockito.mock;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("fake-selector")
public class MockLoadBalancerProvider implements LoadBalancerProvider<FakeSelectorConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(FakeSelectorConfiguration config, ServiceDiscovery serviceDiscovery) {
        return mock(LoadBalancer.class);
    }
}
