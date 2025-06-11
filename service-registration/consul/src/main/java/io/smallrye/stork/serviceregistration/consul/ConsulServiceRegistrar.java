package io.smallrye.stork.serviceregistration.consul;

import static io.smallrye.stork.impl.ConsulMetadataKey.META_CONSUL_SERVICE_ID;

import java.util.ArrayList;
import java.util.List;

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
        client = ConsulClient.create(vertx, options);

    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<ConsulMetadataKey> metadata, String ipAddress,
            int defaultPort) {
        checkAddressNotNull(ipAddress);

        String consulId = metadata.getMetadata().isEmpty() ? serviceName
                : metadata.getMetadata().get(ConsulMetadataKey.META_CONSUL_SERVICE_ID).toString();

        List<String> tags = new ArrayList<>();
        ServiceOptions serviceOptions = new ServiceOptions().setId(consulId).setName(serviceName).setTags(tags)
                .setAddress(ipAddress).setPort(defaultPort);

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
