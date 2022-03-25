package io.smallrye.stork.test;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("these-arent-the-droids-you-are-looking-for")
public class WeirdTypedServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<TheseArentTheDroidsYouAreLookingForConfiguration> {
    @Override
    public ServiceDiscovery createServiceDiscovery(TheseArentTheDroidsYouAreLookingForConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return () -> Uni.createFrom().item(() -> AnchoredServiceDiscoveryProvider.services);
    }
}
