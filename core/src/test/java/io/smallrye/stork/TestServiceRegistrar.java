package io.smallrye.stork;

import java.util.List;

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
    public Uni<Void> registerServiceInstance(String serviceName, String instanceName, Metadata<TestMetadataKey> metadata,
            String ipAddress, int defaultPort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, String instanceName, List<String> tags,
            Metadata<TestMetadataKey> metadata,
            String ipAddress,
            int defaultPort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        throw new UnsupportedOperationException();
    }
}
