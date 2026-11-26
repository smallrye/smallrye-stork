package io.smallrye.stork.serviceregistration.eureka;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarType(value = "eureka", metadataKey = EurekaMetadataKey.class)
@ServiceRegistrarAttribute(name = "eureka-host", description = "The Eureka host.", defaultValue = "localhost")
@ServiceRegistrarAttribute(name = "eureka-port", description = "The Eureka port.", defaultValue = "8761")
@ServiceRegistrarAttribute(name = "eureka-context-path", description = "The Eureka server root context path.", defaultValue = "/")
@ServiceRegistrarAttribute(name = "eureka-trust-all", description = "Enable/Disable the TLS certificate verification", defaultValue = "false")
@ServiceRegistrarAttribute(name = "eureka-tls", description = "Use TLS to connect to the Eureka server", defaultValue = "false")
@ServiceRegistrarAttribute(name = "health-check-url", description = "The liveness http address.", defaultValue = "")
@ServiceRegistrarAttribute(name = "host-name", description = "The host name to register in Eureka. If not set, the ipAddress is used.", defaultValue = "")
@ServiceRegistrarAttribute(name = "secure-port", description = "The secure (HTTPS) port to register in Eureka. If set, the secure port is registered as enabled. If not set, no secure port is registered.", defaultValue = "")
@ServiceRegistrarAttribute(name = "secure-vip-address", description = "The secure virtual IP address to register in Eureka together with the secure port. If not set, the virtual IP address (the service name) is used.", defaultValue = "")
@ServiceRegistrarAttribute(name = "non-secure-port-enabled", description = "Whether the non-secure port is registered as enabled. Set it to false for services only reachable over HTTPS; requires secure-port.", defaultValue = "true")
@ApplicationScoped
public class EurekaServiceRegistrarProvider
        implements ServiceRegistrarProvider<EurekaRegistrarConfiguration, EurekaMetadataKey> {

    @Override
    public ServiceRegistrar<EurekaMetadataKey> createServiceRegistrar(EurekaRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new EurekaServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
