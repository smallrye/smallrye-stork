package io.smallrye.stork.servicediscovery.consul;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceInstanceIds;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;

public class ConsulServiceDiscovery implements ServiceDiscovery {

    private final ConsulClient client;
    private final String serviceName;

    public ConsulServiceDiscovery(String serviceName, ServiceDiscoveryConfig config, Vertx vertx) {
        this.serviceName = serviceName;

        ConsulClientOptions options = new ConsulClientOptions();
        Map<String, String> parameters = config.parameters();
        String host = parameters.get("consul-host");
        if (host != null) {
            options.setHost(host);
        }
        String port = parameters.get("consul-port");
        if (port != null) {
            try {
                options.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port not parseable to int: " + port + " for service " + serviceName);
            }
        }

        client = ConsulClient.create(vertx, options);
    }

    public Uni<List<ServiceInstance>> getServiceInstances() {
        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, true) // TODO: a property to configure t his!
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));
        return serviceEntryList.onItem().transform(this::map); // TODO: logging
    }

    private List<ServiceInstance> map(ServiceEntryList serviceEntryList) {
        List<ServiceEntry> list = serviceEntryList.getList();
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
        for (ServiceEntry serviceEntry : list) {
            // TODO: separate address and port in the ServiceInstance
            String address = String.format("%s:%s", serviceEntry.getService().getAddress(),
                    serviceEntry.getService().getPort());
            // TODO: reuse service instance IDs on refresh (so that they don't change)
            ServiceInstance serviceInstance = new ServiceInstance(ServiceInstanceIds.next(), address);
            serviceInstances.add(serviceInstance);
        }
        return serviceInstances;
    }
}
