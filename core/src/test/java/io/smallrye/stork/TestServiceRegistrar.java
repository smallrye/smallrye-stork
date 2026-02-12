package io.smallrye.stork;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;

public class TestServiceRegistrar implements ServiceRegistrar<TestMetadataKey> {
    private static final Logger log = Logger.getLogger(TestServiceRegistrar.class);
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

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return null;
    }
}
