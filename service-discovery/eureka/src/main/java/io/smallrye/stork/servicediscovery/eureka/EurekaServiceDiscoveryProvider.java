package io.smallrye.stork.servicediscovery.eureka;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class EurekaServiceDiscoveryProvider implements ServiceDiscoveryProvider {
    @Override
    public String type() {
        return "eureka";
    }

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName) {
        return new EurekaServiceDiscovery(config, serviceName);
    }
}
