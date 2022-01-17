package io.smallrye.stork.test;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("fake")
public class AnchoredServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<AnchoredServiceDiscoveryProviderConfiguration> {

    static final List<ServiceInstance> services = new ArrayList<>();

    @Override
    public ServiceDiscovery createServiceDiscovery(AnchoredServiceDiscoveryProviderConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return () -> Uni.createFrom().item(() -> services);
    }
}
