package io.smallrye.stork.serviceregistration.eureka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class EurekaServiceRegistrar implements ServiceRegistrar<EurekaMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(EurekaServiceRegistrar.class);
    private final EurekaRegistrarConfiguration config;
    private final WebClient client;
    private final String path;

    public EurekaServiceRegistrar(EurekaRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        Vertx vertx = infrastructure.get(Vertx.class, Vertx::vertx);
        this.config = config;
        // Eureka instance
        String host = config.getEurekaHost();
        int port = Integer.parseInt(config.getEurekaPort());
        boolean trustAll = Boolean.parseBoolean(config.getEurekaTrustAll());
        boolean eurekaTls = Boolean.parseBoolean(config.getEurekaTls());
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(host).setDefaultPort(port).setSsl(eurekaTls).setTrustAll(trustAll));
        String contextPath = config.getEurekaContextPath();
        if (!contextPath.endsWith("/")) {
            contextPath += "/";
        }
        path = contextPath + "eureka/apps/";
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<EurekaMetadataKey> metadata, String ipAddress,
            int defaultPort) {

        checkAddressNotNull(ipAddress);

        String eurekaId = metadata.getMetadata().isEmpty() ? serviceName
                : metadata.getMetadata().get(EurekaMetadataKey.META_EUREKA_SERVICE_ID).toString();

        return registerApplicationInstance(client, serviceName,
                eurekaId, ipAddress, null,
                defaultPort, null, -1, "UP", "");

    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return client.get("/eureka/apps/" + serviceName)
                .putHeader("Accept", "application/json;charset=UTF-8")
                .send().invoke(() -> log.info("Instance found for '{}'", serviceName))
                .flatMap(item -> {
                    JsonObject body = item.bodyAsJsonObject();
                    JsonObject application = body.getJsonObject("application");
                    JsonObject instance = application.getJsonArray("instance").getJsonObject(0);
                    return deregisterApplicationInstance(application.getString("name"), instance.getString("instanceId"));
                });

    }

    private Uni<Void> deregisterApplicationInstance(String applicationId, String instanceId) {
        return client.delete("/eureka/apps/" + applicationId + "/" + instanceId)
                .putHeader("Accept", "application/xml")
                .send()
                .onFailure()
                .invoke(err -> log.error("Unable to deregister '{}' of '{}'. Error: {}", instanceId, applicationId,
                        err.getMessage()))
                .onItem().invoke(resp -> log.info("'" + instanceId + "'" + " successfully deregistered")).replaceWithVoid();

    }

    private Uni<Void> registerApplicationInstance(WebClient client, String applicationId, String instanceId,
            String ipAddress, String virtualAddress, int port,
            String secureVirtualAddress, int securePort, String state, String path) {
        JsonObject instance = new JsonObject();
        JsonObject registration = new JsonObject();
        instance.put("instance", registration);
        registration
                .put("hostName", "localhost")
                .put("instanceId", instanceId)
                .put("app", applicationId)
                .put("ipAddr", ipAddress)
                .put("vipAddress", virtualAddress)
                .put("port", new JsonObject().put("$", port).put("@enabled", "true"));

        if (secureVirtualAddress != null) {
            registration
                    .put("secureVipAddress", secureVirtualAddress);
        }
        if (config.getHealthCheckUrl() != null && !config.getHealthCheckUrl().isBlank()) {
            registration.put("healthCheckUrl", config.getHealthCheckUrl());
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

        Uni<Void> response = client.post(path + "/eureka/apps/" + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJson(instance)
                .invoke(() -> log.info("Instance registered for service {}: {}", applicationId, registration))
                .replaceWithVoid();

        return response;

    }

}
