package io.smallrye.stork.spi;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.integration.StorkInfrastructure;

public interface ServiceDiscoveryProvider extends ElementWithType {
    ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure);
}
