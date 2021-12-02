package examples;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.integration.StorkInfrastructure;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class AcmeServiceDiscoveryProvider implements ServiceDiscoveryProvider {
    @Override
    public String type() {
        return "acme";
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config,
                                                   String serviceName,
                                                   ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new AcmeServiceDiscovery(config.parameters());
    }
}
