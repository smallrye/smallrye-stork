package io.smallrye.dux.test;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.ServiceDiscoveryConfig;
import io.smallrye.dux.spi.ServiceDiscoveryProvider;

public class TestServiceDiscovery1Provider implements ServiceDiscoveryProvider {

    public static final String TYPE = "test-sd-1";

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config) {
        return new TestServiceDiscovery(config, TYPE);
    }

    @Override
    public String type() {
        return TYPE;
    }
}
