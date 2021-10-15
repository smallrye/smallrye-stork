package examples;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class AcmeServiceDiscoveryProvider implements ServiceDiscoveryProvider {
    @Override
    public String type() {
        return "acme";
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config,
                                                   String serviceName) {
        return new AcmeServiceDiscovery(config.parameters());
    }
}
