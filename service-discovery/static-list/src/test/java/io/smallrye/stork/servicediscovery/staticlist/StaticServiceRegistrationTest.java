package io.smallrye.stork.servicediscovery.staticlist;

import static io.smallrye.stork.servicediscovery.staticlist.StaticListServiceRegistrar.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class StaticServiceRegistrationTest {

    Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static", "static",
                null, Map.of("address-list", "localhost:8080, localhost:8081"),
                Map.of("address-list", "localhost:8080, localhost:8081"));

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @AfterEach
    void clear() {
        StaticAddressesBackend.clear("first-service");
    }

    @Test
    void shouldGetAllServiceInstances() {
        String serviceName = "first-service";
        List<ServiceInstance> serviceInstances = stork.getService(serviceName)
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081);
        assertThat(serviceInstances.stream().map(ServiceInstance::isSecure)).allSatisfy(b -> assertThat(b).isFalse());

        ServiceRegistrar<Metadata.DefaultMetadataKey> staticRegistrar = stork.getService(serviceName).getServiceRegistrar();

        staticRegistrar.registerServiceInstance(serviceName, "remotehost", 9090);

        serviceInstances = stork.getService(serviceName)
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(3);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost", "remotehost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081, 9090);
    }
}
