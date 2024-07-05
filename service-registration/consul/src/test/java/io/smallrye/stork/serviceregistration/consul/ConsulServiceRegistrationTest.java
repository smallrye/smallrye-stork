package io.smallrye.stork.serviceregistration.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceEntryList;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class ConsulServiceRegistrationTest {

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
}
