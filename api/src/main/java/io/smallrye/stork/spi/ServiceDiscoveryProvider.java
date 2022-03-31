package io.smallrye.stork.spi;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;

/**
 * A service discovery provider allowing to create instances of service discovery.
 * <p>
 * Implementation should use the {@link io.smallrye.stork.api.config.ServiceDiscoveryAttribute} to define attributes.
 *
 * @param <T> the configuration type (class generated from the {@link io.smallrye.stork.api.config.ServiceDiscoveryAttribute}
 *        annotations).
 */
public interface ServiceDiscoveryProvider<T> {

    /**
     * Creates a new instance of {@link ServiceDiscovery}.
     *
     * @param config the configuration, must not be {@code null}
     * @param serviceName the service name, must not be {@code null}, or blank.
     * @param serviceConfig the service config, must not be {@code null}
     * @param storkInfrastructure the stork infrastructure, must not be {@code null}
     * @return the service discovery instance
     */
    ServiceDiscovery createServiceDiscovery(T config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure);
}
