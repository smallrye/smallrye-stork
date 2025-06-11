package io.smallrye.stork.serviceregistration.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.Check;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.ServiceOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class ConsulServiceRegistrationTest {

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
    void shouldRegisterConsulIdDefaultingToServiceName() {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);
        consulRegistrar.registerServiceInstance(serviceName, "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, true)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));

        UniAssertSubscriber<ServiceEntryList> subscriber = serviceEntryList
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.awaitItem().getItem()).isNotNull();
    }

    @Test
    void shouldRegisterServiceInstancesInConsul() throws InterruptedException {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);
        consulRegistrar.registerServiceInstance(serviceName, Metadata.of(ConsulMetadataKey.class)
                .with(ConsulMetadataKey.META_CONSUL_SERVICE_ID, serviceName), "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, true)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));

        UniAssertSubscriber<ServiceEntryList> subscriber = serviceEntryList
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(subscriber.awaitItem().getItem()).isNotNull();

    }

    @Test
    void shouldRegisterServiceInstancesUsingOptionsInConsul() throws InterruptedException {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);
        ServiceRegistrar.RegistrarOptions registrarOptions = new ServiceRegistrar.RegistrarOptions(serviceName, "10.96.96.231",
                8406, List.of("v1.2.3", "canary"), Map.of("protocol", "https",
                        "max_connections", "100", "team", "platform"));
        consulRegistrar.registerServiceInstance(registrarOptions).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, true)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));

        UniAssertSubscriber<ServiceEntryList> subscriber = serviceEntryList
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ServiceEntryList item = subscriber.awaitItem().getItem();
        assertThat(item).isNotNull();
        item.getList().stream().findFirst().ifPresent(service -> {
            assertThat(service.getService().getTags()).containsExactlyInAnyOrder("v1.2.3", "canary");
            assertThat(service.getService().getMeta()).containsAllEntriesOf(Map.of("protocol", "https",
                    "max_connections", "100", "team", "platform"));
        });

    }

    @Test
    void shouldRegisterServiceInstancesWithHealthCheckInConsul() throws InterruptedException {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "health-check-url",
                        "/q/health/live", "health-check-interval", "10s",
                        "health-check-deregister-after", "30s"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);
        consulRegistrar.registerServiceInstance(serviceName, Metadata.of(ConsulMetadataKey.class)
                .with(ConsulMetadataKey.META_CONSUL_SERVICE_ID, serviceName), "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, false)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));

        UniAssertSubscriber<ServiceEntryList> subscriber = serviceEntryList
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ServiceEntryList item = subscriber.awaitItem().getItem();
        assertThat(item).isNotNull();
        item.getList().stream().findFirst().ifPresent(service -> {
            assertThat(service.getChecks()).isNotEmpty();
            assertThat(service.getChecks().size()).isEqualTo(2);
            assertThat(service.getChecks().stream().map(Check::getServiceId)).containsAnyOf("my-service");
        });
    }

    @Test
    void shouldFailIfNoIpAddressProvided() throws InterruptedException {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            consulRegistrar.registerServiceInstance(serviceName, Metadata.of(ConsulMetadataKey.class)
                    .with(ConsulMetadataKey.META_CONSUL_SERVICE_ID, serviceName), null, 8406);
        });

        String expectedMessage = "Parameter ipAddress should be provided.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    void shouldDeregisterServiceInstancesInConsul() throws InterruptedException {
        //Given a service `my-service` registered in consul
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"),
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        stork = StorkTestUtils.getNewStorkInstance();
        List<String> tags = List.of("primary");
        registerService(new ConsulServiceOptions(serviceName, 8406, tags, List.of("example.com")));

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);
        consulRegistrar.deregisterServiceInstance(serviceName).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, true)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));

        UniAssertSubscriber<ServiceEntryList> subscriber = serviceEntryList
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ServiceEntryList item = subscriber.awaitItem().getItem();
        assertThat(item.getList()).isEmpty();

    }

    public record ConsulServiceOptions(String serviceName, int port, List<String> tags, List<String> addresses) {
    }

    // Quick registering only for deregister test purpose
    private void registerService(ConsulServiceOptions consulServiceOptions) throws InterruptedException {
        if (consulServiceOptions.addresses().isEmpty()) {
            consulServiceOptions.addresses.add("");
        }
        CountDownLatch latch = new CountDownLatch(consulServiceOptions.addresses().size());
        for (String address : consulServiceOptions.addresses()) {
            client.registerService(
                    new ServiceOptions().setId(consulServiceOptions.serviceName()).setName(consulServiceOptions.serviceName())
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

}
