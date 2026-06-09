package io.smallrye.stork.serviceregistration.staticlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
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

    @Test
    void shouldDeregisterSingleInstanceWithoutAffectingOthers() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "localhost", 8080);
        staticRegistrar.registerServiceInstance(serviceName, "localhost", 8081);
        staticRegistrar.registerServiceInstance(serviceName, "remotehost", 9090);

        assertThat(InMemoryAddressesBackend.getAddresses(serviceName)).hasSize(3);

        staticRegistrar.deregisterServiceInstance(serviceName, "localhost", 8081);

        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);
        assertThat(addresses).hasSize(2);
        assertThat(addresses).containsExactlyInAnyOrder("localhost:8080", "remotehost:9090");
    }

    @Test
    void shouldRegisterWithCustomInstanceNameIgnoringName() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "my-custom-id", Metadata.of(Metadata.DefaultMetadataKey.class),
                "localhost", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);
        assertThat(addresses).hasSize(1);
        assertThat(addresses).contains("localhost:8080");
    }

    @Test
    void shouldRegisterInstanceViaService() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService("first-service");
        service.registerInstance("localhost", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses("first-service");
        assertThat(addresses).hasSize(1);
        assertThat(addresses).contains("localhost:8080");
    }

    @Test
    void shouldRegisterInstanceWithNameViaService() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService("first-service");
        service.registerInstance("my-instance", "remotehost", 9090);

        List<String> addresses = InMemoryAddressesBackend.getAddresses("first-service");
        assertThat(addresses).hasSize(1);
        assertThat(addresses).contains("remotehost:9090");
    }

    @Test
    void shouldRegisterInstanceWithTagsViaService() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService("first-service");
        service.registerInstance("my-instance", List.of("v1.0", "canary"), "localhost", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses("first-service");
        assertThat(addresses).hasSize(1);
        assertThat(addresses).contains("localhost:8080");
    }

    @Test
    void shouldDeregisterInstanceViaService() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService("first-service");
        service.registerInstance("localhost", 8080);
        service.registerInstance("remotehost", 9090);

        assertThat(InMemoryAddressesBackend.getAddresses("first-service")).hasSize(2);

        service.deregisterServiceInstance("localhost", 8080);

        List<String> addresses = InMemoryAddressesBackend.getAddresses("first-service");
        assertThat(addresses).hasSize(1);
        assertThat(addresses).contains("remotehost:9090");
    }

    @Test
    void shouldFailIfAddresseNull() {
        TestConfigProvider.addServiceConfig("first-service", null, null, "static",
                null, null, null);

        stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "first-service";

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "http://localhost:8081/hello", 8080);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            staticRegistrar.registerServiceInstance(serviceName, null, 8080);
        });

        String expectedMessage = "Parameter ipAddress should be provided.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }
}
