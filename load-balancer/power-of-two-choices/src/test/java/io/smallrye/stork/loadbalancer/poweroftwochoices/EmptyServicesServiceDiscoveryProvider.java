package io.smallrye.stork.loadbalancer.poweroftwochoices;

import java.util.Collections;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("empty-services")
public class EmptyServicesServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<EmptyServicesServiceDiscoveryProviderConfiguration> {
    @Override
    public ServiceDiscovery createServiceDiscovery(EmptyServicesServiceDiscoveryProviderConfiguration config,
            String serviceName, ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return () -> Uni.createFrom().item(Collections.emptyList());
    }
}
