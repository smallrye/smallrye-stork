package io.smallrye.stork.servicediscovery.dns;

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
 * DNS-based service discovery implementation
 */
@ServiceDiscoveryAttribute(name = "dns-servers", description = "Comma separated list of dns servers. " +
        "Servers can either be in the `server:port` or just `server` form. Use `none` to use the system resolver.", defaultValue = "none")
@ServiceDiscoveryAttribute(name = "hostname", description = "The hostname to look up; if not defined Stork service name will be used.")
@ServiceDiscoveryAttribute(name = "record-type", description = "Type of the DNS record. A, AAAA and SRV records are supported", defaultValue = "SRV")
@ServiceDiscoveryAttribute(name = "port", description = "Port of the service instances. " +
        "Required if the record type is other than SRV.")
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", defaultValue = CachingServiceDiscovery.DEFAULT_REFRESH_INTERVAL)
@ServiceDiscoveryAttribute(name = "secure", description = "Whether the connection with the service should be encrypted with TLS.")
@ServiceDiscoveryAttribute(name = "recursion-desired", description = "Whether DNS recursion is desired", defaultValue = "true")
@ServiceDiscoveryAttribute(name = "resolve-srv", description = "Whether DNS resolution for SRV records is desired", defaultValue = "true")
@ServiceDiscoveryAttribute(name = "dns-timeout", description = "Timeout for DNS queries", defaultValue = "5s")
@ServiceDiscoveryAttribute(name = "fail-on-error", description = "Whether an error in retrieving service instances " +
        "from one of the DNS servers should cause a failure of the discovery attempt.", defaultValue = "false")
@ServiceDiscoveryType("dns")
@ApplicationScoped
public class DnsServiceDiscoveryProvider implements ServiceDiscoveryProvider<DnsConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(DnsConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new DnsServiceDiscovery(serviceName, config, storkInfrastructure.get(Vertx.class, Vertx::vertx));

    }
}
