package io.smallrye.stork.servicediscovery.consul;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;

/**
 * The consul service discovery provider implementation.
 */
@ServiceDiscoveryAttribute(name = "consul-host", description = "The Consul host.", defaultValue = "localhost")
@ServiceDiscoveryAttribute(name = "consul-port", description = "The Consul port.", defaultValue = "8500")
@ServiceDiscoveryAttribute(name = "use-health-checks", description = "Whether to use health check.", defaultValue = "true")
@ServiceDiscoveryAttribute(name = "application", description = "The application name; if not defined Stork service name will be used.")
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", defaultValue = CachingServiceDiscovery.DEFAULT_REFRESH_INTERVAL)
@ServiceDiscoveryAttribute(name = "secure", description = "whether the connection with the service should be encrypted with TLS.")
@ServiceDiscoveryAttribute(name = "ssl", description = "Whether to enable TLS/SSL when connecting to Consul (default: false)", defaultValue = "false")
@ServiceDiscoveryAttribute(name = "trust-store-path", description = "Path to the trust store file used to verify the Consul server certificate", defaultValue = "")
@ServiceDiscoveryAttribute(name = "trust-store-password", description = "Password of the trust store", defaultValue = "")
@ServiceDiscoveryAttribute(name = "key-store-path", description = "Path to the key store file containing the client certificate and private key", defaultValue = "")
@ServiceDiscoveryAttribute(name = "key-store-password", description = "Password of the key store", defaultValue = "")
@ServiceDiscoveryAttribute(name = "verify-host", description = "Whether to enable hostname verification for the Consul TLS connection (default: false)", defaultValue = "false")
@ServiceDiscoveryAttribute(name = "acl-token", description = "Consul ACL token used for authentication when accessing the Consul API", defaultValue = "")
@ServiceDiscoveryType("consul")
@ApplicationScoped
public class ConsulServiceDiscoveryProvider implements ServiceDiscoveryProvider<ConsulConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(ConsulConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new ConsulServiceDiscovery(serviceName, config, storkInfrastructure.get(Vertx.class, Vertx::vertx));

    }
}
