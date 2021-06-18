package io.smallrye.dux.spi;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.ServiceDiscoveryConfig;

public interface ServiceDiscoveryProvider extends ElementWithType {
    ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config);

    String type();
}
