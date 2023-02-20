package io.smallrye.stork.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;

@ExtendWith(WeldJunit5Extension.class)
public class StaticListServiceDiscoveryProgrammaticApiCDITest {

    Stork stork;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            StaticListServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    @BeforeEach
    void setUp() {
        config.clear();
        stork = StorkTestUtils.getNewStorkInstance();
        stork
                .defineIfAbsent("first-service", ServiceDefinition.of(
                        new StaticConfiguration()
                                .withAddressList("localhost:8080, localhost:8081")))
                .defineIfAbsent("second-service", ServiceDefinition.of(
                        new StaticConfiguration().withAddressList("localhost:8082")
                                .withSecure("true")))
                .defineIfAbsent("third-service", ServiceDefinition.of(
                        new StaticConfiguration().withAddressList("localhost:8083")))
                .defineIfAbsent("secured-service", ServiceDefinition.of(
                        new StaticConfiguration().withAddressList("localhost:443, localhost")))
                .defineIfAbsent("shuffle-service", ServiceDefinition.of(
                        new StaticConfiguration()
                                .withAddressList("localhost:8080, localhost:8081").withShuffle("true")));
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

        List<String> list = serviceInstances.stream().map(si -> si.getHost() + ":" + si.getPort()).collect(Collectors.toList());
        assertThat(list).containsExactly("localhost:8080", "localhost:8081");

        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("localhost",
                "localhost");
        assertThat(serviceInstances.stream().map(ServiceInstance::getPort)).containsExactly(8080, 8081);
        assertThat(serviceInstances.stream().map(ServiceInstance::isSecure)).allSatisfy(b -> assertThat(b).isFalse());
    }

    @Test
    void shouldGetAllServiceInstancesWithShuffle() {
        List<ServiceInstance> serviceInstances = stork.getService("shuffle-service")
                .getInstances()
                .await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);

        List<String> list = serviceInstances.stream().map(si -> si.getHost() + ":" + si.getPort()).collect(Collectors.toList());
        assertThat(list).containsExactlyInAnyOrder("localhost:8080", "localhost:8081");

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
        assertThatThrownBy(() -> stork.defineIfAbsent("broken-service", ServiceDefinition.of(
                new StaticConfiguration()
                        .withAddressList("localhost:8080, localhost:8081, , "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not parseable");
    }

}
