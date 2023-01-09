package io.smallrye.stork.servicediscovery.knative;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;

/**
 * Service discovery provider for Knative.
 */
@ServiceDiscoveryType("knative")
@ServiceDiscoveryAttribute(name = "knative-host", description = "The Knative API host.")
@ServiceDiscoveryAttribute(name = "knative-namespace", description = "The namespace of the service. Use all to discover all namespaces.")
@ServiceDiscoveryAttribute(name = "application", description = "The Knative application Id; if not defined Stork service name will be used.")
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", defaultValue = CachingServiceDiscovery.DEFAULT_REFRESH_INTERVAL)
@ServiceDiscoveryAttribute(name = "secure", description = "Whether the connection with the service should be encrypted with TLS.")
public class KnativeServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<KnativeConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(KnativeConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new KnativeServiceDiscovery(serviceName, config, storkInfrastructure.get(Vertx.class, Vertx::vertx));

    }
}
