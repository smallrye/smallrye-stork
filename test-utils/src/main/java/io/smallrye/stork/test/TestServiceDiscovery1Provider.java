package io.smallrye.stork.test;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryAttribute(name = "one", description = "no description")
@ServiceDiscoveryAttribute(name = "two", description = "no description")
@ServiceDiscoveryType(TestServiceDiscovery1Provider.TYPE)
public class TestServiceDiscovery1Provider implements ServiceDiscoveryProvider<TestSd1Configuration> {

    public static final String TYPE = "test-sd-1";

    @Override
    public ServiceDiscovery createServiceDiscovery(TestSd1Configuration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        return new TestServiceDiscovery(config, TYPE);
    }

}
