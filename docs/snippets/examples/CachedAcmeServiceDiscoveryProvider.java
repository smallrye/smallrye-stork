package examples;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import jakarta.enterprise.context.ApplicationScoped;

@ServiceDiscoveryType("cached-acme")
@ServiceDiscoveryAttribute(name = "host",
        description = "Host name of the service discovery server.", required = true)
@ServiceDiscoveryAttribute(name = "port",
        description = "Hort of the service discovery server.", required = false)
@ServiceDiscoveryAttribute(name = "refresh-period",
        description = "Service discovery cache refresh period.",
        defaultValue = CachingServiceDiscovery.DEFAULT_REFRESH_INTERVAL)
@ApplicationScoped
public class CachedAcmeServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<CachedAcmeConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(
            CachedAcmeConfiguration config,
            String serviceName,
            ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {
        return new CachedAcmeServiceDiscovery(config);
    }
}
