package io.smallrye.stork.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class StaticListServiceDiscoveryTest {

    Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static",
                null, Map.of("1", "http://localhost:8080", "2", "http://localhost:8081"));

        TestConfigProvider.addServiceConfig("second-service", null, "static",
                null, Map.of("3", "http://localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "static",
                null, Map.of("4", "http://localhost:8083"));

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldGetAllServiceInstances() {
        List<ServiceInstance> serviceInstances = Stork.getInstance().getService("first-service")
                .getServiceInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081);
    }

}
