package io.smallrye.stork.servicediscovery.kubernetes;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.vertx.core.Vertx;

public class KubernetesServiceDiscoveryProvider implements ServiceDiscoveryProvider {

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
            ServiceConfig serviceConfig) {
        return new KubernetesServiceDiscovery(serviceName, config, Vertx.vertx(), serviceConfig.secure());

    }

    @Override
    public String type() {
        return "kubernetes";
    }
}
