package io.smallrye.stork.spi;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceDiscoveryConfig;

public interface ServiceDiscoveryProvider extends ElementWithType {
    ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName);

    String type();
}
