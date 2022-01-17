package io.smallrye.stork.spi;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;

public interface ServiceDiscoveryProvider<T> {
    ServiceDiscovery createServiceDiscovery(T config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure);
}
