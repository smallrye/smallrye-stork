package io.smallrye.stork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;

public class TestServiceRegistrar implements ServiceRegistrar<TestMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(TestServiceRegistrar.class);
    private final TestRegistrarConfiguration config;

    public TestServiceRegistrar(TestRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        this.config = config;

    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<TestMetadataKey> metadata, String ipAddress,
            int defaultPort) {
        return null;
    }
}
