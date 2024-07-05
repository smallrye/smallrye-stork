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

        return registerApplicationInstance(client, serviceName,
                metadata.getMetadata().get(EurekaMetadataKey.META_EUREKA_SERVICE_ID).toString(), "192.5.10.236", "acme.com",
                defaultPort, null, -1, "UP", "");

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
        if (securePort != -1) {
            registration.put("securePort", new JsonObject().put("$", securePort).put("@enabled", "true"));
        }

        registration
                .put("status", state.toUpperCase())
                .put("dataCenterInfo", new JsonObject()
                        .put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                        .put("name", "MyOwn"))
                .put("leaseInfo", new JsonObject().put("renewalIntervalInSecs", 10000).put("durationInSecs", 10000));

        return client.post(path + "/eureka/apps/" + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJson(instance).replaceWithVoid();

    }

}
