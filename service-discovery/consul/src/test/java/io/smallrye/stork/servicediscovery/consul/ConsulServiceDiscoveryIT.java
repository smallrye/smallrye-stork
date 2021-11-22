package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.servicediscovery.consul.ConsulServiceDiscovery.META_CONSUL_SERVICE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    long consulId;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        consulPort = consul.getMappedPort(8500);
        consulId = 0L;
        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M"));
        stork = StorkTestUtils.getNewStorkInstance();
        setUpServices(serviceName, 8406, "example.com");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        deregisterServiceInstances(instances.get());

        setUpServices(serviceName, 8506, "another.example.com");

        // when the consul service discovery is called before the end of refreshing period
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        //Then stork returns the instances from the cache
        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("example.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        setUpServices(serviceName, 8406, "example.com");

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

        deregisterServiceInstances(instances.get());

        // When the refresh interval is reached
        Thread.sleep(5000);

        //the service settings change in consul
        setUpServices(serviceName, 8506, "another.example.com");

        instances.set(null);
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

    @Test
    void shouldDiscoverServiceWithSpecificName() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service"));
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        setUpServices("my-consul-service", 8406, "consul.com");
        setUpServices("another-service", 8606, "another.example.com");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("consul.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        setUpServices(serviceName, 8406, "example.com");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance serviceInstance = instances.get().get(0);
        assertThat(serviceInstance.getHost()).isEqualTo("example.com");
        assertThat(serviceInstance.getPort()).isEqualTo(8406);

        long serviceId = serviceInstance.getId();

        deregisterServiceInstances(instances.get());

        // When the refresh interval is reached
        Thread.sleep(5000);

        //the service settings change in consul
        setUpServices(serviceName, 8406, "example.com", "another.example.com");

        instances.set(null);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        //Then stork gets the instances from consul
        assertThat(instances.get()).hasSize(2);

        Optional<ServiceInstance> exampleCom = instances.get().stream().filter(i -> i.getHost().equals("example.com"))
                .findAny();
        assertThat(exampleCom)
                .isNotEmpty()
                .hasValueSatisfying(instance -> assertThat(instance.getId()).isEqualTo(serviceId));

        Optional<ServiceInstance> anotherExample = instances.get().stream()
                .filter(i -> i.getHost().equals("another.example.com")).findAny();
        assertThat(anotherExample).isNotEmpty()
                .hasValueSatisfying(instance -> assertThat(instance.getId()).isNotEqualTo(serviceId));
    }

    private void setUpServices(String serviceName, int port, String... addresses) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(addresses.length);

        for (String address : addresses) {
            client.registerService(
                    new ServiceOptions().setId("" + (consulId++)).setName(serviceName).setAddress(address).setPort(port))
                    .onComplete(result -> {
                        if (result.failed()) {
                            fail("Failed to register service in Consul", result.cause());
                        }
                        latch.countDown();
                    });
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Failed to register service in consul in time");
        }
    }

    private void deregisterServiceInstances(List<ServiceInstance> serviceInstances) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> consulIds = serviceInstances.stream().map(ServiceInstance::getMetadata)
                .map(metadata -> metadata.get(META_CONSUL_SERVICE_ID))
                .filter(consulId -> consulId instanceof String)
                .map(consulId -> (String) consulId)
                .collect(Collectors.toList());
        for (String id : consulIds) {
            client.deregisterService(id, res -> {
                if (res.succeeded()) {
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

}
