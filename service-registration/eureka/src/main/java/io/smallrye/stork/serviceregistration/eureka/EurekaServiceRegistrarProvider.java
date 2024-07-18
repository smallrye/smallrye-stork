package io.smallrye.stork.serviceregistration.eureka;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@ApplicationScoped
public class EurekaServiceRegistrarProvider
        implements ServiceRegistrarProvider<EurekaRegistrarConfiguration, EurekaMetadataKey> {

    private static final Logger log = LoggerFactory.getLogger(EurekaServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<EurekaMetadataKey> createServiceRegistrar(EurekaRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new EurekaServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
