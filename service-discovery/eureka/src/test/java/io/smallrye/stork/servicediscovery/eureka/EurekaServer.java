package io.smallrye.stork.servicediscovery.eureka;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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
 *
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
            context.close();
        }
    }

    public static void unregisterAll(WebClient client) {
        HttpResponse<Buffer> response = client.get("/eureka/apps")
                .putHeader("Accept", "application/json")
                .sendAndAwait();
        Assertions.assertEquals(200, response.statusCode());
        JsonObject applications = response.bodyAsJsonObject().getJsonObject("applications");
        applications.getJsonArray("application").forEach(app -> {
            JsonObject json = (JsonObject) app;
            String appName = ((JsonObject) app).getString("name");
            List<String> instances = json.getJsonArray("instance").stream().map(o -> (JsonObject) o)
                    .map(j -> j.getString("instanceId")).collect(Collectors.toList());
            for (String instance : instances) {
                HttpResponse<Buffer> deletion = client.delete("/eureka/apps/" + appName + "/" + instance)
                        .putHeader("Accept", "application/json")
                        .sendAndAwait();
                Assertions.assertEquals(200, deletion.statusCode());
            }

        });
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
                        .put("name", "MyOwn"));

        HttpResponse<Buffer> response = client.post("/eureka/apps/" + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJsonObjectAndAwait(instance);

        Assertions.assertEquals(204, response.statusCode());
    }

    static void updateApplicationInstanceStatus(WebClient client, String app, String id, String status) {
        String url = "/eureka/apps/" + app + "/" + id + "/status";
        HttpResponse<Buffer> response = client.put(url)
                .addQueryParam("value", status)
                .putHeader("Accept", "application/json")
                .sendAndAwait();
        Assertions.assertEquals(200, response.statusCode());
    }

}
