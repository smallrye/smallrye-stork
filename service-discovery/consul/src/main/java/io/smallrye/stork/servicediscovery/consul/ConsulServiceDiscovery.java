package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.config.StorkConfigHelper.get;
import static io.smallrye.stork.config.StorkConfigHelper.getBoolean;
import static io.smallrye.stork.config.StorkConfigHelper.getInteger;
import static io.smallrye.stork.config.StorkConfigHelper.getOrDefault;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_ID;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE_ADDRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.CachingServiceDiscovery;
import io.smallrye.stork.DefaultServiceInstance;
import io.smallrye.stork.Metadata;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceInstanceIds;
import io.smallrye.stork.spi.ServiceInstanceUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;

public class ConsulServiceDiscovery extends CachingServiceDiscovery {

    private final ConsulClient client;
    private final String serviceName;
    private final String application;
    private final boolean secure;
    private boolean passing = true; // default true?

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceDiscovery.class);

    public ConsulServiceDiscovery(String serviceName, ServiceDiscoveryConfig config, Vertx vertx, boolean secure) {
        super(config);
        this.serviceName = serviceName;
        this.secure = secure;

        ConsulClientOptions options = new ConsulClientOptions();
        Optional<String> host = get(config, "consul-host");
        if (host.isPresent()) {
            options.setHost(host.get());
        }
        Optional<Integer> port = getInteger(serviceName, config, "consul-port");
        if (port.isPresent()) {
            options.setPort(port.get());
        }
        Optional<Boolean> passingConfig = getBoolean(config, "use-health-checks");
        if (passingConfig.isPresent()) {
            LOGGER.info("Processing Consul use-health-checks configured value: {}", passingConfig);
            passing = passingConfig.get();
        }
        this.application = getOrDefault(config, "application", serviceName);
        client = ConsulClient.create(vertx, options);

    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(application, passing)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));
        return serviceEntryList.onItem().transform(newInstances -> toStorkServiceInstances(newInstances, previousInstances));
    }

    private List<ServiceInstance> toStorkServiceInstances(ServiceEntryList serviceEntryList,
            List<ServiceInstance> previousInstances) {
        List<ServiceEntry> list = serviceEntryList.getList();
        List<ServiceInstance> serviceInstances = new ArrayList<>();

        for (ServiceEntry serviceEntry : list) {
            Service service = serviceEntry.getService();
            Map<String, String> labels = service.getTags().stream().collect(Collectors.toMap(Function.identity(), s -> s));
            Metadata<ConsulMetadataKey> consulMetadata = createConsulMetadata(serviceEntry);
            String address = service.getAddress();
            int port = serviceEntry.getService().getPort();
            if (address == null) {
                throw new IllegalArgumentException("Got null address for service " + serviceName);
            }

            ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, address, port);
            if (matching != null) {
                serviceInstances.add(matching);
            } else {
                ServiceInstance serviceInstance = new DefaultServiceInstance(ServiceInstanceIds.next(),
                        address, port, secure, labels, consulMetadata);
                serviceInstances.add(serviceInstance);
            }
        }
        return serviceInstances;
    }

    private Metadata<ConsulMetadataKey> createConsulMetadata(ServiceEntry service) {
        Metadata<ConsulMetadataKey> consulMetadata = Metadata.of(ConsulMetadataKey.class);
        if (service.getService() != null && service.getService().getId() != null) {
            consulMetadata = consulMetadata.with(META_CONSUL_SERVICE_ID, service.getService().getId());
        }
        if (service.getNode() != null && service.getNode().getName() != null) {
            consulMetadata = consulMetadata.with(META_CONSUL_SERVICE_NODE, service.getNode().getName());
        }
        if (service.getNode() != null && service.getNode().getAddress() != null) {
            consulMetadata = consulMetadata.with(META_CONSUL_SERVICE_NODE_ADDRESS, service.getNode().getAddress());
        }
        return consulMetadata;
    }
}
