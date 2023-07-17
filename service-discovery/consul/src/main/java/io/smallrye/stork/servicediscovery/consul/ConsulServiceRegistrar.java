package io.smallrye.stork.servicediscovery.consul;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;
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
        options.setPort(ConsulServiceDiscovery.getPort(serviceName, config.getConsulPort()));
        client = ConsulClient.create(vertx, options);
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<ConsulMetadataKey> metadata, String ipAddress,
            int defaultPort) {

        String consulId = metadata.getMetadata().get(ConsulMetadataKey.META_CONSUL_SERVICE_ID).toString();

        List<String> tags = new ArrayList<>();
        return Uni.createFrom().emitter(em -> client.registerService(
                new ServiceOptions().setId(consulId).setName(serviceName).setTags(tags)
                        .setAddress(ipAddress).setPort(defaultPort))
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

}
