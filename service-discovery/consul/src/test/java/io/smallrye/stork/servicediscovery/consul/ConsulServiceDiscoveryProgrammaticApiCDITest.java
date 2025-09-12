package io.smallrye.stork.servicediscovery.consul;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
@ExtendWith(WeldJunit5Extension.class)
public class ConsulServiceDiscoveryProgrammaticApiCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            ConsulServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;
    @Container
    public GenericContainer<?> consul = new GenericContainer<>(DockerImageName.parse("consul:1.9"))
            .withExposedPorts(8500);

    Stork stork;
    int consulPort;
    ConsulClient client;

    @BeforeEach
    void setUp() {
        config.clear();
        consulPort = consul.getMappedPort(8500);
        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service";
        config.addServiceConfig("my-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldNotFetchWhenRefreshPeriodNotReached(stork, client, serviceName, tags);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        config.addServiceConfig("my-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenRefreshPeriodReached(stork, client, serviceName, tags);
    }

    @Test
    void shouldRefetchWhenCacheInvalidated() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        config.addServiceConfig("my-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenCacheInvalidated(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithSpecificName() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        config.addServiceConfig("my-consul-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithSpecificName(client, stork, serviceName);
    }

    @Test
    void shouldHandleTheSecureAttribute() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        config.addServiceConfig("my-consul-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M", "secure",
                        "true"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        ConsulServiceDiscoveryTestUtils.shouldHandleTheSecureAttribute(client, stork, serviceName);
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        config.addServiceConfig("my-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldPreserveIdsOnRefetch(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithoutAddress() throws InterruptedException {
        //Given a service `my-consul-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        config.addServiceConfig("my-consul-service", null, "consul",
                null,
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));

        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config.getConfigs().get(0).serviceDiscovery()));
        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithoutAddress(client, stork, serviceName);
    }
}
