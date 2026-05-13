package io.smallrye.stork.serviceregistration.eureka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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
public class EurekaRegistrationTest {

    @Container
    public GenericContainer<?> eureka = new GenericContainer<>(DockerImageName.parse("quay.io/amunozhe/eureka-server:0.3"))
            .withExposedPorts(EUREKA_PORT);

    private static Vertx vertx = Vertx.vertx();
    @AutoClose
    private WebClient client;

    public static final int EUREKA_PORT = 8761;

    public int port;
    public String host;

    @BeforeEach
    public void init() {
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(eureka.getHost())
                .setDefaultPort(eureka.getMappedPort(EUREKA_PORT)));
        port = eureka.getMappedPort(EUREKA_PORT);
        host = eureka.getHost();
    }

    @AfterEach
    public void cleanup() {
        TestConfigProvider.clear();
    }

    @Test
    public void testRegistrationServiceInstances(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port), "health-check-url",
                        "/q/health/live"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, Metadata.of(EurekaMetadataKey.class)
                .with(EurekaMetadataKey.META_EUREKA_SERVICE_ID, serviceName), "myServiceAddress", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        JsonObject jsonServiceInstance = application.getJsonArray("instance").getJsonObject(0);

        assertThat(jsonServiceInstance.getString("instanceId")).isEqualTo("my-service");
        assertThat(jsonServiceInstance.getString("ipAddr")).isEqualTo("myServiceAddress");
        assertThat(jsonServiceInstance.getString("hostName")).isEqualTo("myServiceAddress");
        assertThat(jsonServiceInstance.getString("vipAddress")).isEqualTo("my-service");
        assertThat(jsonServiceInstance.getJsonObject("port").getInteger("$")).isEqualTo(8406);
        assertThat(jsonServiceInstance.getString("healthCheckUrl")).isEqualTo("/q/health/live");

    }

    @Test
    public void testRegistrationServiceInstancesWithOptions(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port), "health-check-url",
                        "/q/health/live"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        ServiceRegistrar.RegistrarOptions registrarOptions = new ServiceRegistrar.RegistrarOptions(serviceName, "localhost",
                8406, List.of(), Map.of("protocol", "https", "max_connections", "100", "team", "platform"));

        eurekaServiceRegistrar.registerServiceInstance(registrarOptions).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        JsonObject jsonServiceInstance = application.getJsonArray("instance").getJsonObject(0);

        assertThat(jsonServiceInstance.getString("instanceId")).isEqualTo("my-service::localhost::8406");
        assertThat(jsonServiceInstance.getString("ipAddr")).isEqualTo("localhost");
        assertThat(jsonServiceInstance.getString("vipAddress")).isEqualTo("my-service");
        assertThat(jsonServiceInstance.getJsonObject("port").getInteger("$")).isEqualTo(8406);
        assertThat(jsonServiceInstance.getString("healthCheckUrl")).isEqualTo("/q/health/live");
        assertThat(jsonServiceInstance.getJsonObject("metadata").getString("protocol")).isEqualTo("https");
        assertThat(jsonServiceInstance.getJsonObject("metadata").getString("max_connections")).isEqualTo("100");
        assertThat(jsonServiceInstance.getJsonObject("metadata").getString("team")).isEqualTo("platform");

    }

    @Test
    public void shouldRegisterEurekaIdDefaultingToServiceName(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "localhost", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        JsonObject jsonServiceInstance = application.getJsonArray("instance").getJsonObject(0);

        assertThat(jsonServiceInstance.getString("instanceId")).isEqualTo("my-service::localhost::8406");
        assertThat(jsonServiceInstance.getString("ipAddr")).isEqualTo("localhost");
        assertThat(jsonServiceInstance.getString("vipAddress")).isEqualTo("my-service");
        assertThat(jsonServiceInstance.getJsonObject("port").getInteger("$")).isEqualTo(8406);

    }

    @Test
    void shouldFailIfNoIpAddressProvided() throws InterruptedException {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            eurekaServiceRegistrar.registerServiceInstance(serviceName, Metadata.of(EurekaMetadataKey.class)
                    .with(EurekaMetadataKey.META_EUREKA_SERVICE_ID, serviceName), null, 8406);
        });

        String expectedMessage = "Parameter ipAddress should be provided.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void shouldDeregisterEurekaIdDefaultingToServiceName(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "localhost", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        JsonObject jsonServiceInstance = application.getJsonArray("instance").getJsonObject(0);

        assertThat(jsonServiceInstance.getString("instanceId")).isEqualTo("my-service::localhost::8406");
        assertThat(jsonServiceInstance.getString("ipAddr")).isEqualTo("localhost");
        assertThat(jsonServiceInstance.getString("vipAddress")).isEqualTo("my-service");
        assertThat(jsonServiceInstance.getJsonObject("port").getInteger("$")).isEqualTo(8406);
        CountDownLatch deregistrationLatch = new CountDownLatch(1);
        eurekaServiceRegistrar.deregisterServiceInstance(serviceName, "localhost", 8406)
                .subscribe()
                .with(success -> deregistrationLatch.countDown(), failure -> fail("Failure: " + failure.getMessage()));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> deregistrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response2 = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber2 = response2
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse2 = subscriber2.awaitItem().getItem();
        assertThat(httpResponse2).isNotNull();
        assertThat(httpResponse2.statusCode()).isEqualTo(404);

    }

    @Test
    public void shouldRegisterMultipleInstancesWithUniqueEurekaIds(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(2);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register first instance"));

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "10.96.96.232", 8407).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register second instance"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        assertThat(application.getJsonArray("instance")).hasSize(2);
    }

    @Test
    public void shouldDeregisterSpecificInstanceWhileKeepingOthers(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(2);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register first instance"));

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "10.96.96.232", 8407).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register second instance"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        CountDownLatch deregistrationLatch = new CountDownLatch(1);
        eurekaServiceRegistrar.deregisterServiceInstance(serviceName, "10.96.96.231", 8406)
                .subscribe()
                .with(success -> deregistrationLatch.countDown(), failure -> fail("Failure: " + failure.getMessage()));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> deregistrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        assertThat(application.getJsonArray("instance")).hasSize(1);

        JsonObject remainingInstance = application.getJsonArray("instance").getJsonObject(0);
        assertThat(remainingInstance.getString("instanceId")).isEqualTo("my-service::10.96.96.232::8407");
    }

    @Test
    public void shouldRegisterWithCustomInstanceName(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "my-custom-id", Metadata.of(EurekaMetadataKey.class),
                "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register instance"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        JsonObject instance = application.getJsonArray("instance").getJsonObject(0);
        assertThat(instance.getString("instanceId")).isEqualTo("my-custom-id");
        assertThat(instance.getString("ipAddr")).isEqualTo("10.96.96.231");
    }

    @Test
    public void shouldDeregisterByInstanceName(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(2);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "instance-one", Metadata.of(EurekaMetadataKey.class),
                "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register first instance"));

        eurekaServiceRegistrar.registerServiceInstance(serviceName, "instance-two", Metadata.of(EurekaMetadataKey.class),
                "10.96.96.232", 8407).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail("Failed to register second instance"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        CountDownLatch deregistrationLatch = new CountDownLatch(1);
        eurekaServiceRegistrar.deregisterServiceInstance(serviceName, "instance-one")
                .subscribe()
                .with(success -> deregistrationLatch.countDown(), failure -> fail("Failure: " + failure.getMessage()));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> deregistrationLatch.getCount() == 0L);

        Uni<HttpResponse<Buffer>> response = client.get("/eureka/apps/my-service")
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        UniAssertSubscriber<HttpResponse<Buffer>> subscriber = response
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        HttpResponse<Buffer> httpResponse = subscriber.awaitItem().getItem();
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);

        JsonObject jsonResponse = httpResponse.bodyAsJsonObject();
        JsonObject application = jsonResponse.getJsonObject("application");
        assertThat(application.getJsonArray("instance")).hasSize(1);

        JsonObject remainingInstance = application.getJsonArray("instance").getJsonObject(0);
        assertThat(remainingInstance.getString("instanceId")).isEqualTo("instance-two");
    }

    @Test
    public void shouldCompleteSuccessfullyWhenDeregisteringNonExistentService(TestInfo info) {
        String serviceName = "non-existent-service";
        TestConfigProvider.addServiceConfig(serviceName, null, null, "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        // Deregistering a service that was never registered should complete without error,
        // not throw a NullPointerException when parsing the 404 response body.
        UniAssertSubscriber<Void> subscriber = eurekaServiceRegistrar.deregisterServiceInstance(serviceName)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem().assertCompleted();
    }

}
