package io.smallrye.stork.servicediscovery.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;

@Testcontainers
public class ConsulServiceDiscoveryIT {
    @Container
    public GenericContainer consul = new GenericContainer(DockerImageName.parse("consul:1.9"))
            .withExposedPorts(8500);

    Stork stork;
    int consulPort;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        consulPort = consul.getMappedPort(8500);
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldGetServiceFromConsul() throws InterruptedException {
        String serviceName = "my-service";
        setUpService(serviceName, "example.com", 8406);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("example.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);
    }

    private void setUpService(String serviceName, String address, int port) throws InterruptedException {
        ConsulClient client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));

        CountDownLatch latch = new CountDownLatch(1);
        client.registerService(new ServiceOptions().setName(serviceName).setAddress(address).setPort(port))
                .onComplete(result -> {
                    if (result.failed()) {
                        fail("Failed to register service in Consul", result.cause());
                    }
                    latch.countDown();
                });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Failed to register service in consul in time");
        }
    }
}
