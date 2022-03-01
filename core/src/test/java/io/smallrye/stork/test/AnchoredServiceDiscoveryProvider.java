package io.smallrye.stork.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("fake")
@ServiceDiscoveryAttribute(name = "secure", description = "mark the service secured")
public class AnchoredServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<AnchoredServiceDiscoveryProviderConfiguration> {

    static final List<ServiceInstance> services = new ArrayList<>();

    @Override
    public ServiceDiscovery createServiceDiscovery(AnchoredServiceDiscoveryProviderConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        if ("true".equalsIgnoreCase(config.getSecure())) {
            return () -> Uni.createFrom().item(() -> services.stream().map(si -> {
                ServiceInstance instance = mock(ServiceInstance.class);
                when(instance.isSecure()).thenReturn(true);
                return instance;
            }).collect(Collectors.toList()));
        }
        return () -> Uni.createFrom().item(() -> services);
    }
}
