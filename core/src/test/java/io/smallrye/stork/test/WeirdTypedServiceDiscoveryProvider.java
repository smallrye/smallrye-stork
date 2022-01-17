package io.smallrye.stork.test;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("these-arent-the-droids-youre-looing-for")
public class WeirdTypedServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<WeirdTypedServiceDiscoveryProviderConfiguration> {
    @Override
    public ServiceDiscovery createServiceDiscovery(WeirdTypedServiceDiscoveryProviderConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return () -> Uni.createFrom().item(() -> AnchoredServiceDiscoveryProvider.services);
    }
}
