package io.smallrye.stork.serviceregistration.eureka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class EurekaServiceRegistrar implements ServiceRegistrar<EurekaMetadataKey> {
    private static final Logger log = Logger.getLogger(EurekaServiceRegistrar.class);
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

        String eurekaId = metadata.getMetadata().isEmpty()
                ? buildEurekaId(serviceName, ipAddress, defaultPort)
                : metadata.getMetadata().get(EurekaMetadataKey.META_EUREKA_SERVICE_ID).toString();

        return registerApplicationInstance(client, serviceName,
                eurekaId, ipAddress, null,
                defaultPort, null, -1, "UP", null);

    }

    @Override
    public Uni<Void> registerServiceInstance(RegistrarOptions options) {
        checkRegistrarOptionsNotNull(options);
        checkAddressNotNull(options.ipAddress());

        String eurekaId = buildEurekaId(options.serviceName(), options.ipAddress(), options.defaultPort());
        return registerApplicationInstance(client, options.serviceName(),
                eurekaId, options.ipAddress(), null,
                options.defaultPort(), null, -1, "UP", options.metadata());

    }

    /**
     * Deregisters all instances of the given service from Eureka by querying the application endpoint for their IDs
     * and deregistering them in parallel. This avoids silent failures that would occur if the service ID
     * were assumed to match the service name.
     */
    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return client.get(path + serviceName)
                .putHeader("Accept", "application/json;charset=UTF-8")
                .send()
                .flatMap(item -> {
                    if (item.statusCode() == 404) {
                        log.infof("No application found for '%s'", serviceName);
                        return Uni.createFrom().voidItem();
                    }
                    if (item.statusCode() >= 400) {
                        log.errorf("Eureka returned %d when querying instances for '%s'", item.statusCode(), serviceName);
                        return Uni.createFrom().failure(new RuntimeException(
                                "Eureka returned " + item.statusCode() + " when querying instances for '" + serviceName + "'"));
                    }
                    JsonObject application = item.bodyAsJsonObject().getJsonObject("application");
                    String appName = application.getString("name");
                    JsonArray instances = application.getJsonArray("instance");
                    List<Uni<Void>> deregistrations = new ArrayList<>();
                    for (int i = 0; i < instances.size(); i++) {
                        String instanceId = instances.getJsonObject(i).getString("instanceId");
                        deregistrations.add(deregisterApplicationInstance(appName, instanceId));
                    }
                    return Uni.combine().all().unis(deregistrations).discardItems();
                });
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, String instanceName, Metadata<EurekaMetadataKey> metadata,
            String ipAddress, int defaultPort) {
        checkAddressNotNull(ipAddress);
        String eurekaId = instanceName != null && !instanceName.isEmpty()
                ? instanceName
                : buildEurekaId(serviceName, ipAddress, defaultPort);
        return registerApplicationInstance(client, serviceName, eurekaId, ipAddress, null, defaultPort, null, -1, "UP",
                metadata == null ? Metadata.empty().asMap() : metadata.asMap());
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName, String instanceName) {
        checkInstanceNameNotNull(instanceName);
        return deregisterApplicationInstance(serviceName, instanceName);
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName, String ipAddress, int port) {
        String eurekaId = buildEurekaId(serviceName, ipAddress, port);
        return deregisterApplicationInstance(serviceName, eurekaId);
    }

    private Uni<Void> deregisterApplicationInstance(String applicationId, String instanceId) {
        return client.delete(path + applicationId + "/" + instanceId)
                .putHeader("Accept", "application/xml")
                .send()
                .onFailure()
                .invoke(err -> log.errorf("Unable to deregister '%s' of '%s'. Error: %s", instanceId, applicationId,
                        err.getMessage()))
                .flatMap(resp -> {
                    if (resp.statusCode() >= 400) {
                        log.errorf("Eureka returned %d when deregistering '%s' of '%s'", resp.statusCode(), instanceId,
                                applicationId);
                        return Uni.createFrom().failure(new RuntimeException(
                                "Eureka returned " + resp.statusCode() + " when deregistering '" + instanceId + "' of '"
                                        + applicationId + "'"));
                    }
                    log.infof("'%s' successfully deregistered", instanceId);
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> registerApplicationInstance(WebClient client, String applicationId, String instanceId,
            String ipAddress, String virtualAddress, int port,
            String secureVirtualAddress, int securePort, String state, Map<String, String> metadata) {
        String hostName = config.getHostName() != null && !config.getHostName().isBlank() ? config.getHostName() : ipAddress;
        JsonObject instance = new JsonObject();
        JsonObject registration = new JsonObject();
        instance.put("instance", registration);
        registration
                .put("hostName", hostName)
                .put("instanceId", instanceId)
                .put("app", applicationId)
                .put("ipAddr", ipAddress)
                .put("vipAddress", virtualAddress != null ? virtualAddress : applicationId)
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

        if (metadata != null && !metadata.isEmpty()) {
            JsonObject jsonMetadata = new JsonObject();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                jsonMetadata.put(entry.getKey(), entry.getValue());
            }
            registration.put("metadata", jsonMetadata);
        }
        registration
                .put("status", state.toUpperCase())
                .put("dataCenterInfo", new JsonObject()
                        .put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                        .put("name", "MyOwn"))
                .put("leaseInfo", new JsonObject().put("renewalIntervalInSecs", 10000).put("durationInSecs", 10000));

        Uni<Void> response = client.post(path + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJson(instance)
                .invoke(() -> log.infof("Instance registered for service %s: %s", applicationId, registration))
                .replaceWithVoid();

        return response;

    }

    // '::' is used as separator; it does not appear in well-formed DNS service names, IP addresses, or port numbers.
    private static String buildEurekaId(String serviceName, String ipAddress, int port) {
        return serviceName + "::" + ipAddress + "::" + port;
    }

}
