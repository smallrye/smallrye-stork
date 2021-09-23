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
        //Given a service `my-service` registered in consul
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refreshPeriod", "10"));
        stork = StorkTestUtils.getNewStorkInstance();
        setUpService(serviceName, "example.com", 8406);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        setUpService(serviceName, "another.example.com", 8506);

        // when the consul service discovery is called before the end of refreshing period
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        //Then stork returns the instances from the cache
        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("example.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service` registered in consul
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "3"));
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        setUpService(serviceName, "example.com", 8406);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("example.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);

        deregisterService(serviceName);

        // When the refresh interval is reached
        Thread.sleep(5000);

        //the service settings change in consul
        setUpService(serviceName, "another.example.com", 8506);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        //Then stork gets the instances from consul
        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("another.example.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8506);
    }

    private void setUpService(String serviceName, String address, int port) throws InterruptedException {
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

    private void deregisterService(String serviceName) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        client.deregisterService(serviceName, res -> {
            if (res.succeeded()) {
                System.out.println("Service successfully deregistered");
                latch.countDown();
            } else {
                res.cause().printStackTrace();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Failed to deregister service in consul in time");
        }
    }

}
