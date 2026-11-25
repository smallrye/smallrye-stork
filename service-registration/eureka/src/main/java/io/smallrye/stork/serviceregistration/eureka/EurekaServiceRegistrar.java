package io.smallrye.stork.serviceregistration.eureka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.EurekaMetadataKey;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.DurationUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class EurekaServiceRegistrar implements ServiceRegistrar<EurekaMetadataKey> {
    private static final Logger log = Logger.getLogger(EurekaServiceRegistrar.class);
    private final EurekaRegistrarConfiguration config;
    private final Vertx vertx;
    private final WebClient client;
    private final String path;
    private final int securePort;
    private final String secureVipAddress;
    private final boolean nonSecurePortEnabled;
    private final boolean leaseRenewalEnabled;
    private final Duration leaseRenewalInterval;
    private final Duration leaseDuration;
    // The instances registered by this registrar, by upper-case application id (as Eureka stores it) and instance id
    private final Map<String, Lease> leases = new ConcurrentHashMap<>();

    public EurekaServiceRegistrar(EurekaRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        vertx = infrastructure.get(Vertx.class, Vertx::vertx);
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
        securePort = config.getSecurePort().isBlank() ? -1 : Integer.parseInt(config.getSecurePort());
        secureVipAddress = config.getSecureVipAddress().isBlank() ? null : config.getSecureVipAddress();
        nonSecurePortEnabled = Boolean.parseBoolean(config.getNonSecurePortEnabled());
        if (!nonSecurePortEnabled && securePort == -1) {
            throw new IllegalArgumentException("'non-secure-port-enabled' is set to false but no 'secure-port' is configured");
        }
        leaseRenewalEnabled = Boolean.parseBoolean(config.getLeaseRenewalEnabled());
        leaseRenewalInterval = DurationUtils.parseDuration(config.getLeaseRenewalInterval(), "lease-renewal-interval");
        leaseDuration = DurationUtils.parseDuration(config.getLeaseDuration(), "lease-duration");
        if (leaseRenewalInterval.toMillis() <= 0) {
            throw new IllegalArgumentException(
                    "'lease-renewal-interval' must be greater than 0, but was '" + config.getLeaseRenewalInterval() + "'");
        }
        if (leaseRenewalInterval.compareTo(leaseDuration) >= 0) {
            throw new IllegalArgumentException("'lease-renewal-interval' (" + config.getLeaseRenewalInterval()
                    + ") must be lower than 'lease-duration' (" + config.getLeaseDuration() + ")");
        }
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
                defaultPort, secureVipAddress, securePort, "UP", null);

    }

    @Override
    public Uni<Void> registerServiceInstance(RegistrarOptions options) {
        checkRegistrarOptionsNotNull(options);
        checkAddressNotNull(options.ipAddress());

        String eurekaId = buildEurekaId(options.serviceName(), options.ipAddress(), options.defaultPort());
        return registerApplicationInstance(client, options.serviceName(),
                eurekaId, options.ipAddress(), null,
                options.defaultPort(), secureVipAddress, securePort, "UP", options.metadata());

    }

    /**
     * Deregisters all instances of the given service from Eureka by querying the application endpoint for their IDs
     * and deregistering them in parallel. This avoids silent failures that would occur if the service ID
     * were assumed to match the service name.
     */
    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return stopTracking(serviceName, null)
                .chain(() -> client.get(path + serviceName)
                        .putHeader("Accept", "application/json;charset=UTF-8")
                        .send())
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
        return registerServiceInstance(serviceName, instanceName, List.of(), metadata, ipAddress, defaultPort);
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, String instanceName, List<String> tags,
            Metadata<EurekaMetadataKey> metadata,
            String ipAddress,
            int defaultPort) {
        checkAddressNotNull(ipAddress);
        String eurekaId = instanceName != null && !instanceName.isEmpty()
                ? instanceName
                : buildEurekaId(serviceName, ipAddress, defaultPort);
        Map<String, String> metaMap = new HashMap<>(metadata != null ? metadata.asMap() : Map.of());
        if (tags != null && !tags.isEmpty()) {
            metaMap.put("tags", String.join(",", tags));
        }
        return registerApplicationInstance(client, serviceName, eurekaId, ipAddress, null, defaultPort, null, -1, "UP",
                metaMap);

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
        return stopTracking(applicationId, instanceId)
                .chain(() -> client.delete(path + applicationId + "/" + instanceId)
                        .putHeader("Accept", "application/xml")
                        .send())
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
        String vipAddress = virtualAddress != null ? virtualAddress : applicationId;
        JsonObject instance = new JsonObject();
        JsonObject registration = new JsonObject();
        instance.put("instance", registration);
        registration
                .put("hostName", hostName)
                .put("instanceId", instanceId)
                .put("app", applicationId)
                .put("ipAddr", ipAddress)
                .put("vipAddress", vipAddress)
                .put("port", new JsonObject().put("$", port).put("@enabled", String.valueOf(nonSecurePortEnabled)));

        if (config.getHealthCheckUrl() != null && !config.getHealthCheckUrl().isBlank()) {
            registration.put("healthCheckUrl", config.getHealthCheckUrl());
        }
        if (securePort != -1) {
            registration
                    .put("securePort", new JsonObject().put("$", securePort).put("@enabled", "true"))
                    .put("secureVipAddress", secureVirtualAddress != null ? secureVirtualAddress : vipAddress);
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
                .put("leaseInfo", new JsonObject()
                        .put("renewalIntervalInSecs", Math.max(1, leaseRenewalInterval.toSeconds()))
                        .put("durationInSecs", Math.max(1, leaseDuration.toSeconds())));

        Uni<Void> response = sendRegistration(applicationId, instance)
                .onSubscription().invoke(() -> track(applicationId, instanceId, instance))
                .invoke(() -> log.infof("Instance registered for service %s: %s", applicationId, registration))
                .replaceWithVoid();

        return response;

    }

    private Uni<HttpResponse<Buffer>> sendRegistration(String applicationId, JsonObject instance) {
        return client.post(path + applicationId)
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .sendJson(instance);
    }

    /**
     * Tracks a registered instance, so that its lease gets renewed. Tracking starts with the registration request,
     * so that the lease renewal also registers the instance if the initial registration fails.
     */
    private void track(String applicationId, String instanceId, JsonObject instance) {
        Lease lease = new Lease(applicationId, instanceId, instance);
        Lease previous = leases.put(applicationId.toUpperCase(Locale.ROOT) + "/" + instanceId, lease);
        if (previous != null) {
            previous.stop();
        }
        if (leaseRenewalEnabled) {
            lease.start();
        }
    }

    /**
     * Stops tracking the given instance, or all instances of the application if {@code instanceId} is
     * {@code null}. The returned Uni completes once their renewals in progress are done, so that none of them
     * registers an instance again after its deregistration.
     */
    private Uni<Void> stopTracking(String applicationId, String instanceId) {
        return Uni.createFrom().deferred(() -> {
            List<Uni<Void>> renewalsInProgress = new ArrayList<>();
            leases.forEach((key, lease) -> {
                if (lease.applicationId.equalsIgnoreCase(applicationId)
                        && (instanceId == null || lease.instanceId.equals(instanceId))
                        && leases.remove(key, lease)) {
                    renewalsInProgress.add(lease.stop());
                }
            });
            return renewalsInProgress.isEmpty()
                    ? Uni.createFrom().voidItem()
                    : Uni.combine().all().unis(renewalsInProgress).discardItems();
        });
    }

    /**
     * The lease of a registered instance. Renews it periodically and registers the instance again if Eureka does
     * not know it anymore, e.g. after a restart or an eviction.
     */
    private final class Lease {
        private final String applicationId;
        private final String instanceId;
        private final JsonObject instance;
        private final AtomicReference<CompletableFuture<Void>> renewalInProgress = new AtomicReference<>();
        private volatile boolean active = true;
        private volatile long timerId = -1;

        private Lease(String applicationId, String instanceId, JsonObject instance) {
            this.applicationId = applicationId;
            this.instanceId = instanceId;
            this.instance = instance;
        }

        private void start() {
            timerId = vertx.setPeriodic(leaseRenewalInterval.toMillis(), this::renew);
        }

        /**
         * Stops the renewals. The returned Uni completes once a renewal in progress is done.
         */
        private Uni<Void> stop() {
            active = false;
            vertx.cancelTimer(timerId);
            CompletableFuture<Void> renewal = renewalInProgress.get();
            return renewal == null ? Uni.createFrom().voidItem() : Uni.createFrom().completionStage(renewal);
        }

        private void renew(long id) {
            if (!active) {
                // Stopped before the timer id was known
                vertx.cancelTimer(id);
                return;
            }
            CompletableFuture<Void> renewal = new CompletableFuture<>();
            if (!renewalInProgress.compareAndSet(null, renewal)) {
                // The previous renewal is still in progress
                return;
            }
            client.put(path + applicationId + "/" + instanceId)
                    .send()
                    .chain(response -> {
                        if (response.statusCode() == 404 && active) {
                            log.infof("Eureka does not know '%s' of '%s', registering it again", instanceId, applicationId);
                            return sendRegistration(applicationId, instance);
                        }
                        return Uni.createFrom().item(response);
                    })
                    .subscribe().with(response -> {
                        if (response.statusCode() >= 400 && active) {
                            log.warnf("Eureka returned %d when renewing the lease of '%s' of '%s'", response.statusCode(),
                                    instanceId, applicationId);
                        }
                        complete(renewal);
                    }, failure -> {
                        log.warnf("Unable to renew the lease of '%s' of '%s': %s", instanceId, applicationId,
                                failure.getMessage());
                        complete(renewal);
                    });
        }

        private void complete(CompletableFuture<Void> renewal) {
            renewalInProgress.set(null);
            renewal.complete(null);
        }
    }

    // '::' is used as separator; it does not appear in well-formed DNS service names, IP addresses, or port numbers.
    private static String buildEurekaId(String serviceName, String ipAddress, int port) {
        return serviceName + "::" + ipAddress + "::" + port;
    }

}
