package io.smallrye.stork.servicediscovery.composite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class CompositeServiceDiscoveryTest {

    Stork createStorkWithCompositeService() {
        TestConfigProvider.clear();
        Stork stork = StorkTestUtils.getNewStorkInstance();
        stork
                .defineIfAbsent("first-service",
                        ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8080,localhost:8081")))
                .defineIfAbsent("second-service",
                        ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8082")))
                .defineIfAbsent("third-service",
                        ServiceDefinition.of(new CompositeConfiguration().withServices("first-service, second-service")));
        return stork;
    }

    @Test
    void shouldGetAllServiceInstances() {
        Stork stork = createStorkWithCompositeService();
        List<ServiceInstance> serviceInstances = stork.getService("third-service")
                .getInstances()
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
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(NoSuchServiceDefinitionException.class);

        assertThat(stork.getServiceOptional("missing")).isEmpty();
    }

    @Test
    void shouldHandleServicesDefineLaterButBeforeFirstLookup() {
        TestConfigProvider.clear();
        Stork stork = StorkTestUtils.getNewStorkInstance();
        stork
                .defineIfAbsent("first-service",
                        ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8080,localhost:8081")))
                .defineIfAbsent("third-service",
                        ServiceDefinition.of(new CompositeConfiguration().withServices("first-service, second-service")))
                .defineIfAbsent("second-service",
                        ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8082")));

        List<ServiceInstance> serviceInstances = stork.getService("third-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(3);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost", "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081, 8082);
    }

    @Test
    void shouldFailOnUnknownServiceDuringFirstLookup() {
        TestConfigProvider.clear();
        Stork stork = StorkTestUtils.getNewStorkInstance();
        stork
                .defineIfAbsent("first-service",
                        ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8080,localhost:8081")))
                .defineIfAbsent("third-service",
                        ServiceDefinition.of(new CompositeConfiguration().withServices("first-service, second-service")));

        assertThatThrownBy(() -> stork.getService("third-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5))).isInstanceOf(NoSuchServiceDefinitionException.class)
                .hasMessageContaining("second-service");

        stork.defineIfAbsent("second-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList("localhost:8082")));

        List<ServiceInstance> instances = stork.getService("third-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(3);
        assertThat(instances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost", "localhost");
        assertThat(instances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080,
                8081, 8082);
    }
}
