package io.smallrye.stork.servicediscovery.consul;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class ConsulServiceDiscoveryTest {
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
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldNotFetchWhenRefreshPeriodNotReached(stork, client, serviceName, tags);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenRefreshPeriodReached(stork, client, serviceName, tags);
    }

    @Test
    void shouldRefetchWhenCacheInvalidated() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "60"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();

        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldRefetchWhenCacheInvalidated(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithSpecificName() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithSpecificName(client, stork, serviceName);
    }

    @Test
    void shouldHandleTheSecureAttribute() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service", "secure", "true"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        ConsulServiceDiscoveryTestUtils.shouldHandleTheSecureAttribute(client, stork, serviceName);
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        List<String> tags = List.of("primary");
        ConsulServiceDiscoveryTestUtils.shouldPreserveIdsOnRefetch(client, stork, serviceName, tags);
    }

    @Test
    void shouldDiscoverServiceWithoutAddress() throws InterruptedException {
        //Given a service discovery config for `my-service`
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig(serviceName, null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        ConsulServiceDiscoveryTestUtils.shouldDiscoverServiceWithoutAddress(client, stork, serviceName);
    }

    @Test
    void shouldAcceptSslConfiguration() {
        String serviceName = "my-secure-service";
        TestConfigProvider.addServiceConfig(serviceName, null, "consul", null, null,
                Map.of("consul-host", "localhost",
                        "consul-port", String.valueOf(consulPort),
                        "ssl", "true",
                        "trust-store-path", "/path/to/truststore.jks",
                        "trust-store-password", "changeit",
                        "key-store-path", "/path/to/keystore.jks",
                        "key-store-password", "changeit",
                        "verify-host", "true",
                        "acl-token", "my-acl-token"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        ConsulServiceDiscoveryTestUtils.shouldAcceptSslConfiguration(stork, serviceName);
    }

}
