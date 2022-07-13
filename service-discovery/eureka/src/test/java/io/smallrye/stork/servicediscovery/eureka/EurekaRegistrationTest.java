package io.smallrye.stork.servicediscovery.eureka;

import static io.smallrye.stork.servicediscovery.eureka.EurekaServer.EUREKA_HOST;
import static io.smallrye.stork.servicediscovery.eureka.EurekaServer.EUREKA_PORT;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class EurekaRegistrationTest {

    private static Vertx vertx;
    private WebClient client;

    @BeforeAll
    static void startEureka() {
        vertx = Vertx.vertx();
        EurekaServer.start();
    }

    @AfterAll
    static void stopEureka() {
        vertx.closeAndAwait();
        EurekaServer.stop();
    }

    @BeforeEach
    public void init() {
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(EurekaServer.EUREKA_HOST)
                .setDefaultPort(EurekaServer.EUREKA_PORT));
    }

    @AfterEach
    public void cleanup() {
        EurekaServer.unregisterAll(client);
        TestConfigProvider.clear();
        client.close();

    }

    @Test
    public void testWithoutApplicationInstancesThenOne(TestInfo info) {
        TestConfigProvider.addServiceRegistrarConfig("my-eureka-registrar", "eureka",
                Map.of("eureka-host", EurekaServer.EUREKA_HOST, "eureka-port", String.valueOf(EUREKA_PORT)));
        String serviceName = "my-service";

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getServiceRegistrar("my-eureka-registrar");

        CountDownLatch registrationLatch = new CountDownLatch(1);
        eurekaServiceRegistrar.registerServiceInstance(serviceName, Metadata.of(EurekaMetadataKey.class)
                .with(EurekaMetadataKey.META_EUREKA_SERVICE_ID, serviceName), "acme.com", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

    }

}
