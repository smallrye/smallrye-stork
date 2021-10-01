package io.smallrye.stork.servicediscovery.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.CachingServiceDiscovery;
import io.smallrye.stork.DefaultServiceInstance;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceInstanceIds;
import io.vertx.core.Vertx;

public class KubernetesServiceDiscovery extends CachingServiceDiscovery {

    private final KubernetesClient client;
    private final String serviceName;
    private boolean allNamespaces = false;
    private String namespace;
    private final Vertx vertx;

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);

    public KubernetesServiceDiscovery(String serviceName, ServiceDiscoveryConfig config, Vertx vertx) {
        super(config);
        Config base = Config.autoConfigure(null);
        this.serviceName = serviceName;
        Map<String, String> parameters = config.parameters();
        String masterUrl = parameters != null ? parameters.get("k8s-host") : base.getMasterUrl();
        if (masterUrl == null) {
            masterUrl = base.getMasterUrl();
        }
        namespace = parameters != null ? parameters.get("k8s-namespace") : base.getNamespace();
        if (namespace != null && namespace.equalsIgnoreCase("all")) {
            allNamespaces = true;
        }
        Config properties = new ConfigBuilder(base)
                .withMasterUrl(masterUrl)
                .withNamespace(namespace).build();
        client = new DefaultKubernetesClient(properties);
        this.vertx = vertx;
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances() {
        Uni<List<Endpoints>> endpointsUni = Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(future -> {
                        List<Endpoints> endpoints = new ArrayList<>();
                        if (allNamespaces) {
                            endpoints.addAll(client.endpoints().inAnyNamespace().withField("metadata.name", serviceName).list()
                                    .getItems());
                        } else {
                            endpoints.addAll(
                                    client.endpoints().inNamespace(namespace).withField("metadata.name", serviceName).list()
                                            .getItems());
                        }
                        future.complete(endpoints);
                    }, result -> {
                        if (result.succeeded()) {
                            List<Endpoints> endpoints = (List<Endpoints>) result.result();
                            emitter.complete(endpoints);
                        } else {
                            //TODO logging
                            emitter.fail(result.cause());
                        }
                    });
                });
        return endpointsUni.onItem().transform(this::map);

    }

    public Uni<List<ServiceInstance>> blockingGetServiceInstances() {
        List<Endpoints> endpoints = allNamespaces
                ? client.endpoints().inAnyNamespace().withField("metadata.name", serviceName).list()
                        .getItems()
                : client.endpoints().inNamespace(namespace).withField("metadata.name", serviceName).list().getItems();
        Uni<List<ServiceInstance>> serviceEntryList = Uni.createFrom().item(map(endpoints));
        return serviceEntryList; // TODO: logging
    }

    // TODO review this method and remove it if isn't needed
    public Uni<List<ServiceInstance>> fullAsynchronousGetServiceInstances() {
        Uni<Endpoints> endpointsUni = Uni.createFrom()
                .emitter(emitter -> client.informers().sharedIndexInformerFor(Endpoints.class, 0L).addEventHandler(
                        new ResourceEventHandler<Endpoints>() {
                            @Override
                            public void onAdd(Endpoints obj) {
                                emitter.complete(obj);
                            }

                            @Override
                            public void onUpdate(Endpoints oldObj, Endpoints newObj) {
                            }

                            @Override
                            public void onDelete(Endpoints oldObj, boolean deletedFinalStateUnknown) {
                            }
                        }));
        return endpointsUni.onItem().transform(this::map);
    }

    private List<ServiceInstance> map(List<Endpoints> endpointList) {
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
        for (Endpoints endPoints : endpointList) {
            for (EndpointSubset subset : endPoints.getSubsets()) {
                serviceInstances.addAll(subset.getAddresses().stream().map(endpointAddress -> {
                    String hostname = endpointAddress.getIp();
                    if (hostname == null) { // should we take the hostName?
                        hostname = endpointAddress.getHostname();
                    }
                    List<EndpointPort> endpointPorts = subset.getPorts();
                    Integer port = 0;
                    if (endpointPorts.size() == 1) {
                        port = endpointPorts.get(0).getPort();
                    }
                    return new DefaultServiceInstance(ServiceInstanceIds.next(),
                            hostname, port);
                }).collect(Collectors.toList()));
            }
        }

        return serviceInstances;
    }

    private List<ServiceInstance> map(Endpoints endpoints) {
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
        for (EndpointSubset subset : endpoints.getSubsets()) {
            serviceInstances.addAll(subset.getAddresses().stream().map(endpointAddress -> {
                String hostname = endpointAddress.getIp();
                if (hostname == null) { // should we take the hostName?
                    hostname = endpointAddress.getHostname();
                }
                List<EndpointPort> endpointPorts = subset.getPorts();
                Integer port = 0;
                if (endpointPorts.size() == 1) {
                    port = endpointPorts.get(0).getPort();
                }
                return new DefaultServiceInstance(ServiceInstanceIds.next(),
                        hostname, port);
            }).collect(Collectors.toList()));
        }
        return serviceInstances;
    }
}
