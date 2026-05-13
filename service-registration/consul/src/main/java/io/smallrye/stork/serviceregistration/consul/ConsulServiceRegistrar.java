package io.smallrye.stork.serviceregistration.consul;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceList;
import io.vertx.ext.consul.ServiceOptions;

public class ConsulServiceRegistrar implements ServiceRegistrar<ConsulMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(ConsulServiceRegistrar.class);
    private final Vertx vertx;
    private final ConsulRegistrarConfiguration config;

    private final ConsulClient client;

    public ConsulServiceRegistrar(ConsulRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        vertx = infrastructure.get(Vertx.class, Vertx::vertx);
        this.config = config;

        ConsulClientOptions options = new ConsulClientOptions();
        options.setHost(config.getConsulHost());
        options.setPort(getPort(serviceName, config.getConsulPort()));
        if (config.getSsl().equalsIgnoreCase("true")) {
            options.setSsl(true)
                    .setTrustStoreOptions(new JksOptions()
                            .setPath(config.getTrustStorePath())
                            .setPassword(config.getTrustStorePassword()))
                    .setKeyStoreOptions(new JksOptions()
                            .setPath(config.getKeyStorePath())
                            .setPassword(config.getKeyStorePassword()))
                    .setAclToken(config.getAclToken());
            if (config.getVerifyHost().equalsIgnoreCase("true")) {
                options.setVerifyHost(true);
            }
        }
        client = ConsulClient.create(vertx, options);

    }

    @Override
    public Uni<Void> registerServiceInstance(RegistrarOptions options) {
        checkRegistrarOptionsNotNull(options);
        checkAddressNotNull(options.ipAddress());

        String consulId = buildConsulId(options.serviceName(), options.ipAddress(), options.defaultPort());
        return registerInstance(options.serviceName(), options.ipAddress(), options.defaultPort(), consulId,
                options.tags(), options.metadata());

    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, String instanceName, Metadata<ConsulMetadataKey> metadata,
            String ipAddress,
            int defaultPort) {
        checkAddressNotNull(ipAddress);

        // Use the explicit ID when provided; otherwise generate a unique one.
        String consulId = instanceName != null && !instanceName.isEmpty() ? instanceName
                : buildConsulId(serviceName, ipAddress, defaultPort);
        return registerInstance(serviceName, ipAddress, defaultPort, consulId, List.of(), Collections.emptyMap());
    }

    private Uni<Void> registerInstance(String serviceName, String ipAddress, int defaultPort, String consulId,
            List<String> tags, Map<String, String> metadata) {
        ServiceOptions serviceOptions = new ServiceOptions().setId(consulId).setName(serviceName).setAddress(ipAddress)
                .setPort(defaultPort).setTags(tags).setMeta(metadata);

        if (config.getHealthCheckUrl() != null && !config.getHealthCheckUrl().isBlank()) {
            CheckOptions check = new CheckOptions()
                    .setHttp(config.getHealthCheckUrl());

            if (config.getHealthCheckInterval() != null) {
                check.setInterval(config.getHealthCheckInterval());
            }

            if (config.getHealthCheckDeregisterAfter() != null) {
                check.setDeregisterAfter(config.getHealthCheckDeregisterAfter());
            }
            serviceOptions.setCheckOptions(check);
        }
        return Uni.createFrom().emitter(em -> client.registerService(
                serviceOptions)
                .onComplete(result -> {
                    if (result.failed()) {
                        log.error("Unable to register instance {} of service {}", consulId, serviceName,
                                result.cause());
                        em.fail(result.cause());
                    } else {
                        log.info("Instance {} of service {} has been registered ", consulId, serviceName);
                        em.complete(result.result());
                    }
                }));
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName, String instanceName) {
        checkInstanceNameNotNull(instanceName);
        return Uni.createFrom().emitter(em -> client.deregisterService(instanceName)
                .onComplete(result -> {
                    if (result.failed()) {
                        log.error("Unable to deregister instance {} of service {}", instanceName, serviceName,
                                result.cause());
                        em.fail(result.cause());
                    } else {
                        log.info("Instance {} of service {} has been deregistered", instanceName, serviceName);
                        em.complete(result.result());
                    }
                }));
    }

    /**
     * Deregisters all instances of the given service from Consul by querying the catalog for their IDs
     * and deregistering them in parallel. This avoids silent failures that would occur if the service ID
     * were assumed to match the service name.
     */
    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return Uni.createFrom().<ServiceList> emitter(em -> client.catalogServiceNodes(serviceName).onComplete(result -> {
            if (result.failed()) {
                log.error("Unable to retrieve instances of service {} from Consul catalog", serviceName,
                        result.cause());
                em.fail(result.cause());
            } else {
                em.complete(result.result());
            }
        }))
                .flatMap(serviceList -> {
                    List<Service> services = serviceList.getList();
                    if (services == null || services.isEmpty()) {
                        log.info("No registered instances found for service {}", serviceName);
                        return Uni.createFrom().voidItem();
                    }
                    List<Uni<Void>> deregistrations = services.stream()
                            .map(service -> Uni.createFrom()
                                    .<Void> emitter(em -> client.deregisterService(service.getId()).onComplete(result -> {
                                        if (result.failed()) {
                                            log.error("Unable to deregister instance {} of service {}",
                                                    service.getId(), serviceName, result.cause());
                                            em.fail(result.cause());
                                        } else {
                                            log.info("Instance {} of service {} has been deregistered",
                                                    service.getId(), serviceName);
                                            em.complete(null);
                                        }
                                    })))
                            .collect(Collectors.toList());
                    return Uni.combine().all().unis(deregistrations).discardItems();
                });
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName, String ipAddress, int port) {
        String consulId = buildConsulId(serviceName, ipAddress, port);
        return deregisterServiceInstance(serviceName, consulId);
    }

    // '::' is used as separator; it does not appear in well-formed DNS service names, IP addresses, or port numbers.
    private static String buildConsulId(String serviceName, String ipAddress, int port) {
        return serviceName + "::" + ipAddress + "::" + port;
    }

    protected static Integer getPort(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse the property `consul-port` to an integer from the " +
                    "service discovery configuration for service '" + name + "'", e);
        }
    }

}
