package examples;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ServiceDiscoveryType("acme")
@ServiceDiscoveryAttribute(name = "host",
        description = "Host name of the service discovery server.", required = true)
@ServiceDiscoveryAttribute(name = "port",
        description = "Hort of the service discovery server.", required = false)
@ApplicationScoped
public class AcmeServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<AcmeConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(
            AcmeConfiguration config,
            String serviceName,
            ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {
        return new AcmeServiceDiscovery(config);
    }
}
