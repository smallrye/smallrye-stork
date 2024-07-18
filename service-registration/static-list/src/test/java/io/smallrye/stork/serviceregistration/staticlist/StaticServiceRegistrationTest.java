package io.smallrye.stork.serviceregistration.staticlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.smallrye.stork.utils.InMemoryAddressesBackend;

public class StaticServiceRegistrationTest {

    Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        InMemoryAddressesBackend.clearAll();

    }

    @AfterEach
    void clear() {
        InMemoryAddressesBackend.clear("first-service");
    }

    @Test
    void shouldRegisterServiceInstances() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, Map.of("address-list", "localhost:8080, localhost:8081"));

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "remotehost", 9090);

        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);

        assertThat(addresses).hasSize(3);
        assertThat(addresses.stream()).containsExactlyInAnyOrder("localhost:8080",
                "localhost:8081", "remotehost:9090");
    }

    @Test
    void shouldRegisterServiceInstanceWithEmptyAddressList() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "localhost", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);

        assertThat(addresses).hasSize(1);
        assertThat(addresses.stream()).contains("localhost:8080");
    }

    @Test
    void shouldRegisterServiceInstancesWithSchemeAndPath() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "http://localhost:8081/hello", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);

        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0)).isEqualTo("localhost:8081/hello");
    }
}
