package io.smallrye.stork.servicediscovery.eureka;

import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class EurekaRegistrationTest {

    @Container
    public GenericContainer<?> eureka = new GenericContainer<>(DockerImageName.parse("quay.io/amunozhe/eureka-server:0.2"))
            .withExposedPorts(EUREKA_PORT);
    private static Vertx vertx = Vertx.vertx();
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
        unregisterAll(client);
        TestConfigProvider.clear();
        client.close();

    }

    @Test
    public void testRegistrationServiceInstances(TestInfo info) {
        String serviceName = "my-service";
        TestConfigProvider.addServiceConfig(serviceName, null, "eureka", "eureka", null, null,
                Map.of("eureka-host", eureka.getHost(), "eureka-port", String.valueOf(port)));

        Stork stork = configureAndGetStork(serviceName);

        ServiceRegistrar<EurekaMetadataKey> eurekaServiceRegistrar = stork.getService(serviceName).getServiceRegistrar();

        CountDownLatch registrationLatch = new CountDownLatch(1);

        eurekaServiceRegistrar.registerServiceInstance(serviceName, Metadata.of(EurekaMetadataKey.class)
                .with(EurekaMetadataKey.META_EUREKA_SERVICE_ID, serviceName), "acme.com", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

    }

    private Stork configureAndGetStork(String serviceName) {
        return configureAndGetStork(serviceName, false, null);
    }

    private Stork configureAndGetStork(String serviceName, boolean secure, String instance) {
        Stork stork = StorkTestUtils.getNewStorkInstance();
        EurekaConfiguration configuration = new EurekaConfiguration()
                .withEurekaHost(eureka.getHost())
                .withEurekaPort(Integer.toString(port))
                .withRefreshPeriod("1S")
                .withSecure(Boolean.toString(secure));
        if (instance != null) {
            configuration = configuration.withInstance(instance);
        }
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(configuration));
        return stork;
    }

    private static class ApplicationInstance {
        public final String app;
        public final String instance;

        private ApplicationInstance(String app, String instance) {
            this.app = app;
            this.instance = instance;
        }
    }

    private static final List<ApplicationInstance> instances = new CopyOnWriteArrayList<>();

    public static void unregisterAll(WebClient client) {
        instances.forEach(ai -> client.delete("/eureka/apps/" + ai.app + "/" + ai.instance)
                .putHeader("Accept", "application/json")
                .sendAndAwait());

        instances.clear();
    }

    public static void registerApplicationInstance(WebClient client, String applicationId, String instanceId,
            String virtualAddress, int port,
            String secureVirtualAddress, int securePort, String state, String path) {
        JsonObject instance = new JsonObject();
        JsonObject registration = new JsonObject();
        instance.put("instance", registration);
        registration
                .put("hostName", "localhost")
                .put("instanceId", instanceId)
                .put("app", applicationId)
                .put("ipAddr", "1.1.1." + port)
                .put("vipAddress", virtualAddress)
                .put("port", new JsonObject().put("$", port).put("@enabled", "true"));

        if (secureVirtualAddress != null) {
            registration
                    .put("secureVipAddress", secureVirtualAddress);
        }
        if (securePort != -1) {
            registration.put("securePort", new JsonObject().put("$", securePort).put("@enabled", "true"));
        }

        registration
                .put("status", state.toUpperCase())
                .put("dataCenterInfo", new JsonObject()
                        .put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                        .put("name", "MyOwn"))
                .put("leaseInfo", new JsonObject().put("renewalIntervalInSecs", 10000).put("durationInSecs", 10000));

        HttpResponse<Buffer> response = client.post(path + "/eureka/apps/" + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJsonObjectAndAwait(instance);

        Assertions.assertEquals(204, response.statusCode());
        waitForInstance(client, applicationId, instanceId, path);
        instances.add(new ApplicationInstance(applicationId, instanceId));
    }

    public static void updateApplicationInstanceStatus(WebClient client, String app, String id, String status, String path) {
        String url = path + "/eureka/apps/" + app + "/" + id + "/status";
        await().untilAsserted(() -> {
            HttpResponse<Buffer> response = client.put(url)
                    .addQueryParam("value", status)
                    .putHeader("Accept", "application/json")
                    .sendAndAwait();
            Assertions.assertEquals(200, response.statusCode());
        });
    }

    public static void waitForInstance(WebClient client, String app, String instance, String path) {
        await().untilAsserted(() -> Assertions.assertEquals(200,
                client.get(path + "/eureka/apps/" + app + "/" + instance).sendAndAwait().statusCode()));
    }

}
