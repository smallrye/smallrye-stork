package io.smallrye.stork.test;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryAttribute(name = "three", description = "none")
@ServiceDiscoveryType(TestServiceDiscovery2Provider.TYPE)
public class TestServiceDiscovery2Provider implements ServiceDiscoveryProvider<TestServiceDiscovery2ProviderConfiguration> {

    public static final String TYPE = "test-sd-2";

    @Override
    public ServiceDiscovery createServiceDiscovery(TestServiceDiscovery2ProviderConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new TestServiceDiscovery2(config, TYPE, serviceConfig.secure());
    }
}
