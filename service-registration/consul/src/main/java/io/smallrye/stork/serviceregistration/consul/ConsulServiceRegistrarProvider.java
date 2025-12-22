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
@ServiceRegistrarAttribute(name = "health-check-url", description = "The liveness http address.", defaultValue = "")
@ServiceRegistrarAttribute(name = "health-check-interval", description = "How often Consul performs the health check", defaultValue = "30s")
@ServiceRegistrarAttribute(name = "health-check-deregister-after", description = "How long after the check is in critical status Consul will remove the service from the catalogue.", defaultValue = "1m")
@ServiceRegistrarAttribute(name = "ssl", description = "Whether to enable TLS/SSL when connecting to Consul (default: false)", defaultValue = "false")
@ServiceRegistrarAttribute(name = "trust-store-path", description = "Path to the trust store file used to verify the Consul server certificate", defaultValue = "")
@ServiceRegistrarAttribute(name = "trust-store-password", description = "Password of the trust store", defaultValue = "")
@ServiceRegistrarAttribute(name = "key-store-path", description = "Path to the key store file containing the client certificate and private key", defaultValue = "")
@ServiceRegistrarAttribute(name = "key-store-password", description = "Password of the key store", defaultValue = "")
@ServiceRegistrarAttribute(name = "verify-host", description = "Whether to enable hostname verification for the Consul TLS connection (default: false)", defaultValue = "false")
@ServiceRegistrarAttribute(name = "acl-token", description = "Consul ACL token used for authentication when accessing the Consul API", defaultValue = "")
public class ConsulServiceRegistrarProvider
        implements ServiceRegistrarProvider<ConsulRegistrarConfiguration, ConsulMetadataKey> {

    private static final Logger log = LoggerFactory.getLogger(ConsulServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<ConsulMetadataKey> createServiceRegistrar(ConsulRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new ConsulServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
