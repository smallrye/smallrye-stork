package io.smallrye.stork.test;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.MetadataKey;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarAttribute(name = "one", description = "no description")
@ServiceRegistrarAttribute(name = "two", description = "no description")
@ServiceRegistrarType(value = TestServiceRegistrarProvider.TYPE, metadataKey = TestServiceRegistrarProvider.TestMetadata.class)
public class TestServiceRegistrarProvider
        implements ServiceRegistrarProvider<TestSrRegistrarConfiguration, TestServiceRegistrarProvider.TestMetadata> {

    private static final List<Registration> registrations = new ArrayList<>();
    public static final String TYPE = "test-sr";

    public static void clear() {
        registrations.clear();
    }

    public static List<Registration> getRegistrations() {
        return registrations;
    }

    @Override
    public ServiceRegistrar<TestMetadata> createServiceRegistrar(TestSrRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new TestServiceRegistrar(config, serviceRegistrarName);
    }

    public static class Registration {
        public final String serviceRegistrarName;
        public final TestSrRegistrarConfiguration config;
        public final Metadata<TestMetadata> metadata;
        public final String ipAddress;
        public final int port;
        public final String serviceName;

        public Registration(String serviceRegistrarName, TestSrRegistrarConfiguration config, Metadata<TestMetadata> metadata,
                String serviceName, String ipAddress, int port) {
            this.serviceRegistrarName = serviceRegistrarName;
            this.config = config;
            this.metadata = metadata;
            this.ipAddress = ipAddress;
            this.serviceName = serviceName;
            this.port = port;
        }
    }

    public static class TestServiceRegistrar implements ServiceRegistrar<TestMetadata> {

        TestSrRegistrarConfiguration config;
        String serviceRegistrarName;

        public TestServiceRegistrar(TestSrRegistrarConfiguration config, String serviceRegistrarName) {
            this.config = config;
            this.serviceRegistrarName = serviceRegistrarName;
        }

        @Override
        public Uni<Void> registerServiceInstance(String serviceName, Metadata<TestMetadata> metadata, String ipAddress,
                int defaultPort) {
            registrations.add(new Registration(serviceRegistrarName, config, metadata, serviceName, ipAddress, defaultPort));
            return Uni.createFrom().voidItem();
        }
    }

    public enum TestMetadata implements MetadataKey {
        FIRST("pierwszy");

        private final String name;

        TestMetadata(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
