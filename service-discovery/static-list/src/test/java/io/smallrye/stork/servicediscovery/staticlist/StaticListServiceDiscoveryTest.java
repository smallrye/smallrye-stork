package io.smallrye.stork.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class StaticListServiceDiscoveryTest {

    Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static", null,
                null, Map.of("address-list", "localhost:8080, localhost:8081"), null);

        TestConfigProvider.addServiceConfig("second-service", null, "static", null,
                null, Map.of("address-list", "localhost:8082", "secure", "true"), null);

        TestConfigProvider.addServiceConfig("third-service", null, "static", null,
                null, Map.of("address-list", "localhost:8083"), null);

        TestConfigProvider.addServiceConfig("secured-service", null, "static", null,
                null, Map.of("address-list", "localhost:443, localhost"), null);

        TestConfigProvider.addServiceConfig("scheme-service", null, "static", null,
                null, Map.of("address-list", "http://localhost:8080, https://localhost:8081"), null);

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void testSecureDetection() {
        List<ServiceInstance> instances = stork.getService("secured-service").getInstances().await()
                .atMost(Duration.ofSeconds(5));

        assertThat(instances).hasSize(2);
        assertThat(instances.get(0).isSecure()).isTrue();
        assertThat(instances.get(1).isSecure()).isFalse();

        instances = stork.getService("second-service").getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).isSecure()).isTrue();
    }

    @Test
    void testSchemeDetection() {
        List<ServiceInstance> instances = stork.getService("scheme-service").getInstances().await()
                .atMost(Duration.ofSeconds(5));

        assertThat(instances).hasSize(2);
        assertThat(instances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(instances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081);
    }

    @Test
    void shouldGetAllServiceInstances() {
        List<ServiceInstance> serviceInstances = stork.getService("first-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081);
        assertThat(serviceInstances.stream().map(ServiceInstance::isSecure)).allSatisfy(b -> assertThat(b).isFalse());
    }

    @Test
    void shouldFailOnMissingService() {
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(NoSuchServiceDefinitionException.class);

        assertThat(stork.getServiceOptional("missing")).isEmpty();
    }

    @Test
    void shouldFailOnInvalidFormat() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("broken-service", null, "static", null,
                null, Map.of("address-list", "localhost:8080, localhost:8081, , "), null);
        assertThatThrownBy(StorkTestUtils::getNewStorkInstance).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not parseable");
    }

}
