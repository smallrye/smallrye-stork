package io.smallrye.stork.serviceregistration.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class EurekaLeaseRenewalTest {

    private static final int EUREKA_PORT = 8761;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String SERVICE_NAME = "my-service";
    // Reads the registry directly, unlike /eureka/apps/{app}, which is cached and not refreshed by renewals.
    private static final String INSTANCE_PATH = "/eureka/apps/my-service/my-service::10.96.96.231::8406";

    @Container
    public GenericContainer<?> eureka = new GenericContainer<>(DockerImageName.parse("quay.io/amunozhe/eureka-server:0.3"))
            .withExposedPorts(EUREKA_PORT);

    private static final Vertx vertx = Vertx.vertx();
    @AutoClose
    private WebClient client;
    private ServiceRegistrar<EurekaMetadataKey> registrar;

    @BeforeEach
    void init() {
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(eureka.getHost())
                .setDefaultPort(eureka.getMappedPort(EUREKA_PORT)));
    }

    @AfterEach
    void cleanup() {
        if (registrar != null) {
            registrar.deregisterServiceInstance(SERVICE_NAME).await().atMost(TIMEOUT);
        }
        TestConfigProvider.clear();
    }

    @Test
    void shouldRenewLease() {
        registerInstance();

        JsonObject leaseInfo = getInstance().bodyAsJsonObject().getJsonObject("instance").getJsonObject("leaseInfo");
        assertThat(leaseInfo.getInteger("renewalIntervalInSecs")).isEqualTo(1);
        assertThat(leaseInfo.getInteger("durationInSecs")).isEqualTo(3);
        long registeredAt = leaseInfo.getLong("lastRenewalTimestamp");

        await().atMost(TIMEOUT).until(() -> getInstance().bodyAsJsonObject().getJsonObject("instance")
                .getJsonObject("leaseInfo").getLong("lastRenewalTimestamp") > registeredAt);
    }

    @Test
    void shouldRegisterAgainWhenEurekaDropsTheInstance() {
        registerInstance();

        assertThat(client.delete(INSTANCE_PATH).send().await().atMost(TIMEOUT).statusCode()).isEqualTo(200);

        await().atMost(TIMEOUT).until(() -> getInstance().statusCode() == 200);
    }

    @Test
    void shouldStopRenewingWhenDeregistered() {
        registerInstance();

        registrar.deregisterServiceInstance(SERVICE_NAME).await().atMost(TIMEOUT);
        // Already deregistered: skip cleanup, which would read Eureka's cached app list and get a 404 on DELETE.
        registrar = null;

        await().during(Duration.ofSeconds(3)).atMost(TIMEOUT).until(() -> getInstance().statusCode() == 404);
    }

    private void registerInstance() {
        TestConfigProvider.addServiceConfig(SERVICE_NAME, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(eureka.getMappedPort(EUREKA_PORT)),
                        "lease-renewal-interval", "1s", "lease-duration", "3s"));
        registrar = StorkTestUtils.getNewStorkInstance().getService(SERVICE_NAME).getServiceRegistrar();
        registrar.registerServiceInstance(SERVICE_NAME, "10.96.96.231", 8406).await().atMost(TIMEOUT);
    }

    private HttpResponse<Buffer> getInstance() {
        return client.get(INSTANCE_PATH).putHeader("Accept", "application/json").send().await().atMost(TIMEOUT);
    }
}
