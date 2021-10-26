package io.smallrye.stork.servicediscovery.eureka;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Assertions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * Let's be clear - Starting an Eureka server is not as easy as it should be.
 * The available container image is out of date (https://hub.docker.com/r/springcloud/eureka) and unusable.
 * <p>
 * The easiest way is to start a Spring application.
 * It leads to test classpath pollution as you need to select the versions carefully.
 * <p>
 * This class is responsible for starting and stopping the Eureka server.
 * The spring application reads the `src/test/resources/application.properties` file.
 * <p>
 * This class starts the server and provides helper methods to handle registrations and status updates.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServer {

    private static ConfigurableApplicationContext context;

    public static final String EUREKA_HOST = "localhost";
    public static final int EUREKA_PORT = 8761;
    @SuppressWarnings("HttpUrlsUsage")
    public static final String EUREKA_URL = "http://" + EUREKA_HOST + ":" + EUREKA_PORT;

    public static void start() {
        context = SpringApplication.run(EurekaServer.class);
        await()
                .atMost(Duration.ofSeconds(20))
                .catchUncaughtExceptions()
                .until(() -> RestAssured.get(EUREKA_URL).statusCode() == 200);
    }

    public static void stop() {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {
                // ignored
            }
        }
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
            String secureVirtualAddress, int securePort, String state) {
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

        HttpResponse<Buffer> response = client.post("/eureka/apps/" + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJsonObjectAndAwait(instance);

        Assertions.assertEquals(204, response.statusCode());
        waitForInstance(client, applicationId, instanceId);
        instances.add(new ApplicationInstance(applicationId, instanceId));
    }

    static void updateApplicationInstanceStatus(WebClient client, String app, String id, String status) {
        String url = "/eureka/apps/" + app + "/" + id + "/status";
        await().untilAsserted(() -> {
            HttpResponse<Buffer> response = client.put(url)
                    .addQueryParam("value", status)
                    .putHeader("Accept", "application/json")
                    .sendAndAwait();
            Assertions.assertEquals(200, response.statusCode());
        });
    }

    public static void waitForInstance(WebClient client, String app, String instance) {
        await().untilAsserted(() -> Assertions.assertEquals(200,
                client.get("/eureka/apps/" + app + "/" + instance).sendAndAwait().statusCode()));
    }
}
