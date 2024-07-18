package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_ID;
import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_NODE;
import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_NODE_ADDRESS;
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

import jakarta.inject.Inject;

import org.assertj.core.util.Maps;
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
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;

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
    long consulId;

    @BeforeEach
    void setUp() {
        config.clear();
        consulPort = consul.getMappedPort(8500);
        consulId = 0L;
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
        registerService(serviceName, 8406, tags, "example.com");

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
        registerService(serviceName, 8506, sTags, "another.example.com");

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
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort)).withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        //Given a service `my-service` registered in consul
        List<String> tags = List.of("primary");
        Map<String, String> metadata = Maps.newHashMap("meta", "metadata for my-service");
        registerService(serviceName, 8406, tags, "example.com");

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
        registerService(serviceName, 8506, sTags, "another.example.com");

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
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5M").withApplication("my-consul-service");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        //Given a service `my-service` registered in consul
        registerService("my-consul-service", 8406, null, "consul.com");
        registerService("another-service", 8606, null, "another.example.com");

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
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5M").withApplication("my-consul-service")
                .withSecure("true");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        //Given a service `my-service` registered in consul
        registerService("my-consul-service", 8406, null, "consul.com");
        registerService("another-service", 8606, null, "another.example.com");

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
        ConsulConfiguration config = new ConsulConfiguration().withConsulHost("localhost")
                .withConsulPort(String.valueOf(consulPort))
                .withRefreshPeriod("5");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        List<String> tags = List.of("primary");
        registerService(serviceName, 8406, tags, "example.com");

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
        registerService(serviceName, 8406, tags, "example.com", "another.example.com");

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

    private void registerService(String serviceName, int port, List<String> tags,
            String... addresses) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(addresses.length);
        for (String address : addresses) {
            client.registerService(
                    new ServiceOptions().setId("" + (consulId++)).setName(serviceName).setTags(tags)
                            .setAddress(address).setPort(port))
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
