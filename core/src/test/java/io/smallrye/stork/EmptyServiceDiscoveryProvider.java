package io.smallrye.stork;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("empty")
public class EmptyServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<EmptyConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(EmptyConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return null;
    }
}
