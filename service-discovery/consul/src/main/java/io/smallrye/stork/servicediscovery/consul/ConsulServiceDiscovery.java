package io.smallrye.stork.servicediscovery.consul;

import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_ID;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE;
import static io.smallrye.stork.servicediscovery.consul.ConsulMetadataKey.META_CONSUL_SERVICE_NODE_ADDRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.ServiceInstanceUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;

/**
 * A service discovery implementation retrieving services from Consul.
 */
public class ConsulServiceDiscovery extends CachingServiceDiscovery {

    private final ConsulClient client;
    private final String serviceName;
    private final String application;
    private final boolean secure;
    private final boolean passing;

    ConsulServiceDiscovery(String serviceName, ConsulConfiguration config, Vertx vertx) {
        super(config.getRefreshPeriod());
        this.serviceName = serviceName;
        this.secure = isSecure(config);
        // TODO: more validation
        ConsulClientOptions options = new ConsulClientOptions();
        options.setHost(config.getConsulHost());
        options.setPort(getPort(serviceName, config.getConsulPort()));
        passing = Boolean.parseBoolean(config.getUseHealthChecks());
        this.application = config.getApplication() == null ? serviceName : config.getApplication();
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
            if (address == null || address.isEmpty() || address.isBlank()) {
                address = serviceEntry.getNode().getAddress();
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

    protected static Integer getPort(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse the property `consul-port` to an integer from the " +
                    "service discovery configuration for service '" + name + "'", e);
        }
    }

    private boolean isSecure(ConsulConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }
}
