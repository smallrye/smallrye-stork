package io.smallrye.stork.servicediscovery.consul;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class ConsulServiceDiscoveryProgrammaticApiTest {
    @Container
    public GenericContainer<?> consul = new GenericContainer<>(DockerImageName.parse("consul:1.9"))
            .withExposedPorts(8500);

    Stork stork;
    int consulPort;
    ConsulClient client;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        consulPort = consul.getMappedPort(8500);
        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service";

        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort)).withRefreshPeriod("5M");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldNotFetchWhenRefreshPeriodNotReached(stork, client, serviceName, tags);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort)).withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenRefreshPeriodReached(stork, client, serviceName, tags);
    }

    @Test
    void shouldRefetchWhenCacheInvalidated() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort)).withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenCacheInvalidated(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithSpecificName() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5M").withApplication("my-consul-service");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithSpecificName(client, stork, serviceName);
    }

    @Test
    void shouldHandleTheSecureAttribute() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5M").withApplication("my-consul-service")
                .withSecure("true");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        ConsulServiceDiscoveryTestUtils.shouldHandleTheSecureAttribute(client, stork, serviceName);
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldPreserveIdsOnRefetch(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithoutAddress() throws InterruptedException {
        //Given a service `my-consul-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithoutAddress(client, stork, serviceName);
    }

}
