package io.smallrye.stork.serviceregistration.eureka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;

public class EurekaRegistrarErrorHandlingTest {

    private Vertx vertx;
    private HttpServer mockServer;
    private int port;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        TestConfigProvider.clear();
        if (mockServer != null) {
            mockServer.close().await().indefinitely();
        }
        vertx.close().await().indefinitely();
    }

    private void startMockServerAlwaysReturning(int statusCode) {
        mockServer = vertx.createHttpServer()
                .requestHandler(req -> req.response().setStatusCode(statusCode).endAndForget())
                .listen(0).await().indefinitely();
        port = mockServer.actualPort();
    }

    private void startMockServerWithGetOkDeleteFailing(int deleteStatusCode) {
        String instancesResponse = "{\"application\":{\"name\":\"my-service\",\"instance\":[{\"instanceId\":\"my-service::localhost::8406\"}]}}";
        mockServer = vertx.createHttpServer()
                .requestHandler(req -> {
                    if (req.method() == HttpMethod.GET) {
                        req.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json")
                                .endAndForget(instancesResponse);
                    } else {
                        req.response().setStatusCode(deleteStatusCode).endAndForget();
                    }
                })
                .listen(0).await().indefinitely();
        port = mockServer.actualPort();
    }

    private ServiceRegistrar<EurekaMetadataKey> registrarFor(String serviceName) {
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", "localhost", "eureka-port", String.valueOf(port)));
        Stork stork = StorkTestUtils.getNewStorkInstance();
        return stork.getService(serviceName).getServiceRegistrar();
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 500, 503 })
    void shouldFailWhenEurekaReturnsErrorOnListInstances(int statusCode) {
        startMockServerAlwaysReturning(statusCode);
        UniAssertSubscriber<Void> subscriber = registrarFor("my-service")
                .deregisterServiceInstance("my-service")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        assertThat(subscriber.getFailure().getMessage()).contains(String.valueOf(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 500, 503 })
    void shouldFailWhenEurekaReturnsErrorOnDeleteInstance(int statusCode) {
        startMockServerWithGetOkDeleteFailing(statusCode);
        UniAssertSubscriber<Void> subscriber = registrarFor("my-service")
                .deregisterServiceInstance("my-service", "localhost", 8406)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        assertThat(subscriber.getFailure().getMessage()).contains(String.valueOf(statusCode));
    }
}