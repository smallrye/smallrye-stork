package io.smallrye.stork;

import static org.mockito.Mockito.mock;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("these-arent-the-droids-youre-looking-for-selector")
public class WeirdTypedLoadBalancerProvider
        implements LoadBalancerProvider<TheseArentTheDroidsYoureLookingForSelectorConfiguration> {
    @Override
    public LoadBalancer createLoadBalancer(TheseArentTheDroidsYoureLookingForSelectorConfiguration config,
            ServiceDiscovery serviceDiscovery) {
        return mock(LoadBalancer.class);
    }
}
