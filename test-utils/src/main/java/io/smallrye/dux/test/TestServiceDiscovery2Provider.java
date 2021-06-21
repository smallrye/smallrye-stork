package io.smallrye.dux.test;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.ServiceDiscoveryConfig;
import io.smallrye.dux.spi.ServiceDiscoveryProvider;

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
