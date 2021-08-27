package io.smallrye.stork.servicediscovery.kubernetes;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.vertx.core.Vertx;

public class KubernetesServiceDiscoveryProvider implements ServiceDiscoveryProvider {

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName) {
        return new KubernetesServiceDiscovery(serviceName, config, Vertx.vertx());

    }

    @Override
    public String type() {
        return "kubernetes";
    }
}
