package io.smallrye.stork.servicediscovery.consul;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;

@ServiceDiscoveryAttribute(name = "consul-host", description = "The Consul host.", defaultValue = "localhost")
@ServiceDiscoveryAttribute(name = "consul-port", description = "The Consul port.", defaultValue = "8500")
@ServiceDiscoveryAttribute(name = "use-health-checks", description = "Whether to use health check.", defaultValue = "true")
@ServiceDiscoveryAttribute(name = "application", description = "The application name; if not defined Stork service name will be used.")
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", defaultValue = "5M")
@ServiceDiscoveryAttribute(name = "secure", description = "whether the connection with the service should be encrypted with TLS.")
@ServiceDiscoveryType("consul")
public class ConsulServiceDiscoveryProvider implements ServiceDiscoveryProvider<ConsulConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(ConsulConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new ConsulServiceDiscovery(serviceName, config, storkInfrastructure.get(Vertx.class, Vertx::vertx));

    }
}
