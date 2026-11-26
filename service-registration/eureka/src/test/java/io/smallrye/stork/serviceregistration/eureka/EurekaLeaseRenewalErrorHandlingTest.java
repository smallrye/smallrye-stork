package io.smallrye.stork.serviceregistration.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.core.http.HttpServerRequest;

public class EurekaLeaseRenewalErrorHandlingTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final List<HttpMethod> requests = new CopyOnWriteArrayList<>();
    private Vertx vertx;
    private HttpServer mockServer;
    private ServiceRegistrar<EurekaMetadataKey> registrar;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        if (registrar != null) {
            registrar.deregisterServiceInstance("my-service", "localhost", 8406).await().atMost(TIMEOUT);
        }
        TestConfigProvider.clear();
        if (mockServer != null) {
            mockServer.close().await().indefinitely();
        }
        vertx.close().await().indefinitely();
    }

    @Test
    void shouldKeepRenewingWhenEurekaReturnsAnError() {
        startMockServer(renewal -> renewal.response().setStatusCode(500).endAndForget());
        registerInstance("true");

        await().atMost(TIMEOUT).until(() -> count(HttpMethod.PUT) >= 3);
        assertThat(count(HttpMethod.POST)).isEqualTo(1);
    }

    @Test
    void shouldKeepRenewingWhenTheConnectionFails() {
        startMockServer(renewal -> renewal.connection().closeAndForget());
        registerInstance("true");

        await().atMost(TIMEOUT).until(() -> count(HttpMethod.PUT) >= 3);
        assertThat(count(HttpMethod.POST)).isEqualTo(1);
    }

    @Test
    void shouldNotRegisterAgainWhenDeregisteredDuringRenewal() {
        // Eureka answers the renewal with 404 only after the deregistration has started.
        startMockServer(renewal -> vertx.setTimer(1000, ignored -> renewal.response().setStatusCode(404).endAndForget()));
        registerInstance("true");
        await().atMost(TIMEOUT).until(() -> count(HttpMethod.PUT) == 1);

        registrar.deregisterServiceInstance("my-service", "localhost", 8406).await().atMost(TIMEOUT);

        await().during(Duration.ofSeconds(3)).atMost(TIMEOUT)
                .until(() -> count(HttpMethod.POST) == 1 && count(HttpMethod.PUT) == 1);
        assertThat(requests).last().isEqualTo(HttpMethod.DELETE);
    }

    @Test
    void shouldNotRenewWhenDisabled() {
        startMockServer(renewal -> renewal.response().setStatusCode(200).endAndForget());
        registerInstance("false");

        await().during(Duration.ofSeconds(3)).atMost(TIMEOUT).until(() -> count(HttpMethod.PUT) == 0);
    }

    @ParameterizedTest
    @CsvSource({
            "0s, 90s, must be greater than 0",
            "90s, 90s, must be lower than 'lease-duration'",
            "100s, 90s, must be lower than 'lease-duration'",
            "30s, 0s, must be lower than 'lease-duration'"
    })
    void shouldRejectInvalidLeaseDurations(String leaseRenewalInterval, String leaseDuration, String expectedMessage) {
        TestConfigProvider.addServiceConfig("my-service", null, null, "eureka", null, null,
                Map.of("lease-renewal-interval", leaseRenewalInterval, "lease-duration", leaseDuration));

        assertThatThrownBy(StorkTestUtils::getNewStorkInstance).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    private void startMockServer(Consumer<HttpServerRequest> renewalHandler) {
        mockServer = vertx.createHttpServer()
                .requestHandler(request -> {
                    requests.add(request.method());
                    if (request.method() == HttpMethod.PUT) {
                        renewalHandler.accept(request);
                    } else {
                        request.response().setStatusCode(204).endAndForget();
                    }
                })
                .listen(0).await().indefinitely();
    }

    private void registerInstance(String leaseRenewalEnabled) {
        TestConfigProvider.addServiceConfig("my-service", null, null, "eureka", null, null,
                Map.of("eureka-host", "localhost", "eureka-port", String.valueOf(mockServer.actualPort()),
                        "lease-renewal-enabled", leaseRenewalEnabled, "lease-renewal-interval", "1s"));
        registrar = StorkTestUtils.getNewStorkInstance().getService("my-service").getServiceRegistrar();
        registrar.registerServiceInstance("my-service", "localhost", 8406).await().atMost(TIMEOUT);
    }

    private long count(HttpMethod method) {
        return requests.stream().filter(method::equals).count();
    }
}
