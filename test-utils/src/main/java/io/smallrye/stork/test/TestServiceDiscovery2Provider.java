package io.smallrye.stork.test;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class TestServiceDiscovery2Provider implements ServiceDiscoveryProvider {

    public static final String TYPE = "test-sd-2";

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config) {
        return new TestServiceDiscovery(config, TYPE);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
