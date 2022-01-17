package io.smallrye.stork.spi.internal;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ElementWithType;
import io.smallrye.stork.spi.StorkInfrastructure;

/**
 * Used by stork internals to generate service loader for LoadBalancerProvider
 */
public interface ServiceDiscoveryLoader extends ElementWithType {
    ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure);
}
