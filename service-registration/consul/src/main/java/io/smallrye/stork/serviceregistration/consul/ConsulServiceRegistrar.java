package io.smallrye.stork.serviceregistration.consul;

import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_ID;

import java.util.List;
import java.util.Map;

import io.vertx.core.net.JksOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.impl.ConsulMetadataKey;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
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

        return registerInstance(options.serviceName(), options.ipAddress(), options.defaultPort(), options.serviceName(),
                options.tags(), options.metadata());

    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<ConsulMetadataKey> metadata, String ipAddress,
            int defaultPort) {
        checkAddressNotNull(ipAddress);

        String consulId = metadata.getMetadata().isEmpty() ? serviceName
                : metadata.getMetadata().get(ConsulMetadataKey.META_CONSUL_SERVICE_ID).toString();

        return registerInstance(serviceName, ipAddress, defaultPort, consulId, List.of(), Map.of());
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
                        log.error("Unable to register instances of service {}", serviceName,
                                result.cause());
                        em.fail(result.cause());
                    } else {
                        log.info("Instances of service {} has been registered ", serviceName);
                        em.complete(result.result());
                    }
                }));
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        return Uni.createFrom().emitter(em -> client.deregisterService(serviceName)
                .onComplete(result -> {
                    if (result.failed()) {
                        log.error("Unable to deregister instances of service {}", serviceName,
                                result.cause());
                        em.fail(result.cause());
                    } else {
                        log.info("Instances of service {} has been deregistered ", serviceName);
                        em.complete(result.result());
                    }
                }));
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
