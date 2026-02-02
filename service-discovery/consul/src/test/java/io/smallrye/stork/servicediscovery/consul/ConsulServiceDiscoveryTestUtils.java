package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_ID;
import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_NODE;
import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_NODE_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ServiceOptions;

public class ConsulServiceDiscoveryTestUtils {

    private static long consulId = 0L;

    public static void shouldNotFetchWhenRefreshPeriodNotReached(Stork stork, ConsulClient client, String serviceName,
            List<String> tags)
            throws InterruptedException {

        registerService(client, new ConsulRegisteringOptions(serviceName, 8406, tags, List.of("example.com")));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(10))
                .until(() -> instances.get() != null);

        deregisterServiceInstances(client, instances.get());

        List<String> sTags = List.of("secondary");
        registerService(client,
                new ConsulRegisteringOptions(serviceName, 8506, sTags, List.of("another.example.com")));

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

    public static void shouldRefetchWhenRefreshPeriodReached(Stork stork, ConsulClient client, String serviceName,
            List<String> tags) throws InterruptedException {
        registerService(client, new ConsulRegisteringOptions(serviceName, 8406, tags, List.of("example.com")));

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

        deregisterServiceInstances(client, instances.get());

        //the service settings change in consul
        List<String> sTags = List.of("secondary");
        registerService(client, new ConsulRegisteringOptions(serviceName, 8506, sTags, List.of("another.example.com")));

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

    public static void shouldRefetchWhenCacheInvalidated(ConsulClient client, Stork stork, String serviceName,
            List<String> tags) throws InterruptedException {
        registerService(client, new ConsulRegisteringOptions(serviceName, 8406, tags, List.of("example.com")));

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

        deregisterServiceInstances(client, instances.get());

        //the service settings change in consul
        List<String> sTags = List.of("secondary");
        registerService(client,
                new ConsulRegisteringOptions(serviceName, 8506, sTags, List.of("another.example.com")));

        // the refresh period is not yet finish (60s), the service instance are populated from cache
        await().atMost(Duration.ofSeconds(7))
                .until(() -> service.getServiceDiscovery().getServiceInstances().await().indefinitely().get(0).getHost()
                        .equals("example.com"));

        //Force cache invalidation
        ConsulServiceDiscovery consulServiceDiscovery = (ConsulServiceDiscovery) service.getServiceDiscovery();
        consulServiceDiscovery.invalidate();

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

    public static void shouldDiscoverServiceWithSpecificName(ConsulClient client, Stork stork, String serviceName)
            throws InterruptedException {
        //Given a service `my-service` registered in consul
        registerService(client, new ConsulRegisteringOptions("my-consul-service", 8406, null, List.of("consul.com")));
        registerService(client, new ConsulRegisteringOptions("another-service", 8606, null, List.of("another.example.com")));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(7))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("consul.com");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8406);
        assertThat(instances.get().get(0).isSecure()).isFalse();
    }

    public static void shouldHandleTheSecureAttribute(ConsulClient client, Stork stork, String serviceName)
            throws InterruptedException {
        registerService(client, new ConsulRegisteringOptions("my-consul-service", 8406, null, List.of("consul.com")));
        registerService(client,
                new ConsulRegisteringOptions("another-service", 8606, null, List.of("another.example.com")));

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

    public static void shouldPreserveIdsOnRefetch(ConsulClient client, Stork stork, String serviceName, List<String> tags)
            throws InterruptedException {
        registerService(client, new ConsulRegisteringOptions(serviceName, 8406, tags, List.of("example.com")));

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

        deregisterServiceInstances(client, instances.get());

        //the service settings change in consul
        registerService(client,
                new ConsulRegisteringOptions(serviceName, 8406, tags, List.of("example.com", "another.example.com")));

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

    public static void shouldDiscoverServiceWithoutAddress(ConsulClient client, Stork stork, String serviceName)
            throws InterruptedException {
        registerService(client, new ConsulRegisteringOptions(serviceName, 8406, null, new ArrayList<>()));

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

    public static void registerService(ConsulClient client, ConsulRegisteringOptions ConsulRegisteringOptions)
            throws InterruptedException {
        if (ConsulRegisteringOptions.addresses().isEmpty()) {
            ConsulRegisteringOptions.addresses().add("");
        }
        CountDownLatch latch = new CountDownLatch(ConsulRegisteringOptions.addresses().size());
        for (String address : ConsulRegisteringOptions.addresses()) {
            client.registerService(
                    new ServiceOptions().setId("" + (consulId++)).setName(ConsulRegisteringOptions.serviceName())
                            .setTags(ConsulRegisteringOptions.tags())
                            .setAddress(address).setPort(ConsulRegisteringOptions.port()))
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

    public static void deregisterServiceInstances(ConsulClient client, List<ServiceInstance> serviceInstances)
            throws InterruptedException {
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

    public static void shouldAcceptSslConfiguration(Stork stork, String serviceName) {
        Service service = stork.getService(serviceName);
        assertThat(service).isNotNull();
        assertThat(service.getServiceDiscovery()).isNotNull();
        assertThat(service.getServiceDiscovery()).isInstanceOf(ConsulServiceDiscovery.class);
    }
}
