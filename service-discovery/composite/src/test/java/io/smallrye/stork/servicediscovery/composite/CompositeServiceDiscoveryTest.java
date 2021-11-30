package io.smallrye.stork.servicediscovery.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class CompositeServiceDiscoveryTest {

    Stork createStorkWithCompositeService() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static",
                null, Map.of("1", "localhost:8080", "2", "localhost:8081"));

        TestConfigProvider.addServiceConfig("second-service", null, "static",
                null, Map.of("3", "localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "composite",
                null, Map.of("services", " first-service , second-service"));

        return StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldGetAllServiceInstances() {
        Stork stork = createStorkWithCompositeService();
        List<ServiceInstance> serviceInstances = stork.getService("third-service")
                .getServiceInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(3);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost", "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081, 8082);
    }

    @Test
    void shouldFailForLackOfServicesList() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("composite-service", null, "composite",
                null, Collections.emptyMap());

        assertThatThrownBy(StorkTestUtils::getNewStorkInstance).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailOnEmptyServiceName() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static",
                null, Map.of("1", "localhost:8080", "2", "localhost:8081"));

        TestConfigProvider.addServiceConfig("second-service", null, "static",
                null, Map.of("3", "localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "composite",
                null, Map.of("services", "first-service,,second-service"));

        assertThatThrownBy(StorkTestUtils::getNewStorkInstance).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailOnMissingService() {
        Stork stork = createStorkWithCompositeService();
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(IllegalArgumentException.class);

        assertThat(stork.getServiceOptional("missing")).isEmpty();
    }
}
