package io.smallrye.stork.test;

import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

/**
 * A service discovery implementation returning an empty list of service instances.
 * The list is unmodifiable.
 */
@ServiceDiscoveryType("empty-services")
public class EmptyServicesServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<EmptyServicesConfiguration> {

    private static final List<ServiceInstance> EMPTY = Collections.emptyList();

    @Override
    public ServiceDiscovery createServiceDiscovery(EmptyServicesConfiguration config,
            String serviceName, ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return () -> Uni.createFrom().item(EMPTY);
    }
}
