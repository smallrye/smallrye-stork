package io.smallrye.stork.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
@ApplicationScoped
public class AnchoredServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<FakeConfiguration> {

    static final List<ServiceInstance> services = new ArrayList<>();

    @Inject
    MyDataBean bean;

    @Override
    public ServiceDiscovery createServiceDiscovery(FakeConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        if (bean != null) {
            for (ServiceInstance service : services) {
                when(service.getLabels()).thenReturn(Map.of("label", bean.value()));
            }
        }

        if ("true".equalsIgnoreCase(config.getSecure())) {
            return () -> Uni.createFrom().item(() -> services.stream().map(si -> {
                ServiceInstance instance = mock(ServiceInstance.class);
                when(instance.isSecure()).thenReturn(true);
                if (bean != null) {
                    when(instance.getLabels()).thenReturn(Map.of("label", bean.value()));
                }
                return instance;
            }).collect(Collectors.toList()));
        }
        return () -> Uni.createFrom().item(() -> services);
    }
}
