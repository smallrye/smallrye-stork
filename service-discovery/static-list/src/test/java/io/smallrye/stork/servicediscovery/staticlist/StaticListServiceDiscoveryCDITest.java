package io.smallrye.stork.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;

@ExtendWith(WeldJunit5Extension.class)
public class StaticListServiceDiscoveryCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            StaticListServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;
    Stork stork;

    @BeforeEach
    void setUp() {
        config.clear();
        config.addServiceConfig("first-service", null, "static",
                null, Map.of("address-list", "localhost:8080, localhost:8081"));
        config.addServiceConfig("second-service", null, "static",
                null, Map.of("address-list", "localhost:8082", "secure", "true"));
        config.addServiceConfig("third-service", null, "static",
                null, Map.of("address-list", "localhost:8083"));
        config.addServiceConfig("fourth-service", null, "static",
                null, Map.of("address-list", "localhost:8083/foo, localhost:8083/bar"));
        config.addServiceConfig("secured-service", null, "static",
                null, Map.of("address-list", "localhost:443, localhost"));

        this.stork = StorkTestUtils.getNewStorkInstance();

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
    void shouldParsePath() {
        List<ServiceInstance> serviceInstances = stork.getService("fourth-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8083,
                8083);
        assertThat(serviceInstances.stream().map(ServiceInstance::getPath).map(s -> s.orElse(null))).containsExactlyInAnyOrder(
                "/foo",
                "/bar");
        assertThat(serviceInstances.stream().map(ServiceInstance::isSecure)).allSatisfy(b -> assertThat(b).isFalse());
    }

    @Test
    void shouldFailOnMissingService() {
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(NoSuchServiceDefinitionException.class);

        assertThat(stork.getServiceOptional("missing")).isEmpty();
    }

    @Test
    void shouldFailOnInvalidFormat() {
        config.clear();
        config.addServiceConfig("broken-service", null, "static",
                null, Map.of("address-list", "localhost:8080, localhost:8081, , "));
        Stork.shutdown();
        assertThatThrownBy(StorkTestUtils::getNewStorkInstance).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not parseable");
    }

}
