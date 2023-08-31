package io.smallrye.stork;

import static org.mockito.Mockito.mock;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("mock")
@ServiceDiscoveryAttribute(name = "failure", description = "indicates if service discovery should fail")
@ApplicationScoped
public class MockServiceDiscoveryProvider implements ServiceDiscoveryProvider<MockConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(MockConfiguration config, String serviceName, ServiceConfig serviceConfig,
            StorkInfrastructure storkInfrastructure) {
        return mock(ServiceDiscovery.class);
    }
}
