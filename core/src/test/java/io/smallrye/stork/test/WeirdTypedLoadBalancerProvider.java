package io.smallrye.stork.test;

import static org.mockito.Mockito.mock;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("these-arent-the-droids-youre-looking-for")
public class WeirdTypedLoadBalancerProvider implements LoadBalancerProvider<WeirdTypedLoadBalancerProviderConfiguration> {
    @Override
    public LoadBalancer createLoadBalancer(WeirdTypedLoadBalancerProviderConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return mock(LoadBalancer.class);
    }
}
