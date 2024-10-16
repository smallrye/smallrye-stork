package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_ID;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class ConsulServiceDiscoveryTest {
    @Container
    public GenericContainer<?> consul = new GenericContainer<>(DockerImageName.parse("consul:1.9"))
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
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5M"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        List<String> tags = List.of("primary");
        registerService(new ConsulServiceOptions(serviceName, 8406, tags, List.of("example.com")));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(10))
                .until(() -> instances.get() != null);

        deregisterServiceInstances(instances.get());

        List<String> sTags = List.of("secondary");
        registerService(new ConsulServiceOptions(serviceName, 8506, sTags, List.of("another.example.com")));

        // when the consul service discovery is called before the end of refreshing period
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        //Then stork returns the instances from the cache
        assertThat(instances.get()).hasSize(1);
        ServiceInstance serviceInstance = instances.get().get(0);
        assertThat(serviceInstance.getHost()).isEqualTo("example.com");
        assertThat(serviceInstance.getPort()).isEqualTo(8406);
        assertThat(serviceInstance.getLabels()).containsKey("primary");
        Metadata<ConsulMetadataKey> consulMetadata = (Metadata<ConsulMetadataKey>) serviceInstance.getMetadata();
        assertThat(consulMetadata.getMetadata()).containsKeys(META_CONSUL_SERVICE_ID, META_CONSUL_SERVICE_NODE,
                META_CONSUL_SERVICE_NODE_ADDRESS);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        Map<String, String> metadata = Maps.newHashMap("meta", "metadata for my-service");
        registerService(new ConsulServiceOptions(serviceName, 8406, tags, List.of("example.com")));

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

        //the service settings change in consul
        List<String> sTags = List.of("secondary");
        registerService(new ConsulServiceOptions(serviceName, 8506, sTags, List.of("another.example.com")));

        // let's wait until the new services are populated to Stork (from Consul)
        await().atMost(Duration.ofSeconds(7))
                .until(() -> service.getServiceDiscovery().getServiceInstances().await().indefinitely().get(0).getHost()
                        .equals("another.example.com"));

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
        assertThat(instances.get().get(0).getLabels()).containsKey("secondary");
        Metadata<ConsulMetadataKey> consulMetadata = (Metadata<ConsulMetadataKey>) instances.get().get(0).getMetadata();
        assertThat(consulMetadata.getMetadata()).containsKeys(META_CONSUL_SERVICE_ID, META_CONSUL_SERVICE_NODE,
                META_CONSUL_SERVICE_NODE_ADDRESS);
    }

    @Test
    void shouldDiscoverServiceWithSpecificName() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        registerService(new ConsulServiceOptions("my-consul-service", 8406, null, List.of("consul.com")));
        registerService(new ConsulServiceOptions("another-service", 8606, null, List.of("another.example.com")));

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
        assertThat(instances.get().get(0).isSecure()).isFalse();
    }

    @Test
    void shouldHandleTheSecureAttribute() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service", "secure", "true"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        registerService(new ConsulServiceOptions("my-consul-service", 8406, null, List.of("consul.com")));
        registerService(new ConsulServiceOptions("another-service", 8606, null, List.of("another.example.com")));

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
        assertThat(instances.get().get(0).isSecure()).isTrue();
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {
        //Given a service `my-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig("my-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        registerService(new ConsulServiceOptions(serviceName, 8406, tags, List.of("example.com")));

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

        //the service settings change in consul
        registerService(new ConsulServiceOptions(serviceName, 8406, tags, List.of("example.com", "another.example.com")));

        // let's wait until the new services are populated to Stork (from Consul)
        await().atMost(Duration.ofSeconds(10)).until(
                () -> service.getServiceDiscovery().getServiceInstances().await().atMost(Duration.ofSeconds(5)).size() == 2);

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

    @Test
    void shouldDiscoverServiceWithoutAddress() throws InterruptedException {
        //Given a service `my-consul-service` registered in consul and a refresh-period of 5 seconds
        String serviceName = "my-consul-service";
        TestConfigProvider.addServiceConfig("my-consul-service", null, "consul", null,
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5",
                        "application", "my-consul-service"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();
        registerService(new ConsulServiceOptions("my-consul-service", 8406, null, new ArrayList<>()));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        // Then the service instance is found
        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("127.0.0.1");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);
        assertThat(instances.get().get(0).isSecure()).isFalse();
    }

    public record ConsulServiceOptions(String serviceName, int port, List<String> tags, List<String> addresses) {
    }

    private void registerService(ConsulServiceOptions consulServiceOptions) throws InterruptedException {
        if (consulServiceOptions.addresses().isEmpty()) {
            consulServiceOptions.addresses.add("");
        }
        CountDownLatch latch = new CountDownLatch(consulServiceOptions.addresses().size());
        for (String address : consulServiceOptions.addresses()) {
            client.registerService(
                    new ServiceOptions().setId("" + (consulId++)).setName(consulServiceOptions.serviceName())
                            .setTags(consulServiceOptions.tags())
                            .setAddress(address).setPort(consulServiceOptions.port()))
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
                .map(metadata -> metadata.getMetadata().get(META_CONSUL_SERVICE_ID))
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
