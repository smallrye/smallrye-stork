package io.smallrye.stork.serviceregistration.consul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarType(value = "consul", metadataKey = ConsulMetadataKey.class)
@ServiceRegistrarAttribute(name = "consul-host", description = "The Consul host.", defaultValue = "localhost")
@ServiceRegistrarAttribute(name = "consul-port", description = "The Consul port.", defaultValue = "8500")
@ServiceRegistrarAttribute(name = "consul-health-check-url", description = "The liveness http address.", defaultValue = "")
@ServiceRegistrarAttribute(name = "consul-health-check-interval", description = "How often Consul performs the health check", defaultValue = "30s")
@ServiceRegistrarAttribute(name = "consul-health-check-deregister-after", description = "How long after the check is in critical status Consul will remove the service from the catalogue.", defaultValue = "1M")
public class ConsulServiceRegistrarProvider
        implements ServiceRegistrarProvider<ConsulRegistrarConfiguration, ConsulMetadataKey> {

    private static final Logger log = LoggerFactory.getLogger(ConsulServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<ConsulMetadataKey> createServiceRegistrar(ConsulRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new ConsulServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
