package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_NAMESPACE;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_PORT_PROTOCOL;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.ServiceInstanceUtils;
import io.vertx.core.Vertx;

/**
 * An implementation of service discovery for Kubernetes.
 * This implementation locates a Kubernetes service and retrieves the <em>endpoints</em> (the address of the pods
 * backing the service).
 */
public class KubernetesServiceDiscovery extends CachingServiceDiscovery {

    static final String METADATA_NAME = "metadata.name";
    private final KubernetesClient client;
    private final String application;
    private final String portName;
    private final boolean allNamespaces;
    private final String namespace;
    private final boolean secure;
    private final Vertx vertx;

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);

    private AtomicBoolean invalidated = new AtomicBoolean();

    /**
     * Creates a new KubernetesServiceDiscovery.
     *
     * @param serviceName the service name
     * @param config the configuration
     * @param vertx the vert.x instance
     */
    public KubernetesServiceDiscovery(String serviceName, KubernetesConfiguration config, Vertx vertx) {
        super(config.getRefreshPeriod());
        Config base = Config.autoConfigure(null);
        String masterUrl = config.getK8sHost() == null ? base.getMasterUrl() : config.getK8sHost();
        this.application = config.getApplication() == null ? serviceName : config.getApplication();
        this.namespace = config.getK8sNamespace() == null ? base.getNamespace() : config.getK8sNamespace();
        this.portName = config.getPortName();

        allNamespaces = namespace != null && namespace.equalsIgnoreCase("all");

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace is not configured for service '" + serviceName
                    + "'. Please provide a namespace. Use 'all' to discover services in all namespaces");
        }

        Config k8sConfig = new ConfigBuilder(base)
                .withMasterUrl(masterUrl)
                .withNamespace(namespace).build();
        this.client = new KubernetesClientBuilder().withConfig(k8sConfig).build();
        this.vertx = vertx;
        this.secure = isSecure(config);
        AnyNamespaceOperation<Endpoints, EndpointsList, Resource<Endpoints>> endpointsOperation;
        if (allNamespaces) {
            endpointsOperation = client.endpoints().inAnyNamespace();
        } else {
            endpointsOperation = client.endpoints().inNamespace(namespace);
        }
        endpointsOperation.inform(new ResourceEventHandler<Endpoints>() {
            @Override
            public void onAdd(Endpoints obj) {
                LOGGER.info("Endpoint added: {}", obj.getMetadata().getName());
                invalidate();
            }

            @Override
            public void onUpdate(Endpoints oldObj, Endpoints newObj) {
                LOGGER.info("Endpoint updated : {}", newObj.getMetadata().getName());
                invalidate();
            }

            @Override
            public void onDelete(Endpoints obj, boolean deletedFinalStateUnknown) {
                LOGGER.info("Endpoint deleted: {}", obj.getMetadata().getName());
                invalidate();
            }

        });

    }

    @Override
    public Uni<List<ServiceInstance>> cache(Uni<List<ServiceInstance>> uni) {
        return uni.memoize().until(() -> invalidated.get());
    }

    public void invalidate() {
        invalidated.set(true);
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        Uni<Map<Endpoints, List<Pod>>> endpointsUni = Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(future -> {
                        Map<Endpoints, List<Pod>> items = new HashMap<>();

                        if (allNamespaces) {
                            List<Endpoints> endpointsList = client.endpoints().inAnyNamespace()
                                    .withField(METADATA_NAME, application).list()
                                    .getItems();
                            for (Endpoints endpoint : endpointsList) {
                                List<Pod> backendPods = new ArrayList<>();
                                List<String> podNames = endpoint.getSubsets().stream()
                                        .flatMap(endpointSubset -> endpointSubset.getAddresses().stream())
                                        .map(address -> address.getTargetRef().getName()).collect(Collectors.toList());
                                podNames.forEach(podName -> backendPods
                                        .addAll(client.pods().inAnyNamespace().withField(METADATA_NAME, podName).list()
                                                .getItems()));
                                items.put(endpoint, backendPods);
                            }
                        } else {
                            List<Endpoints> endpointsList = client.endpoints().inNamespace(namespace)
                                    .withField(METADATA_NAME, application)
                                    .list()
                                    .getItems();
                            for (Endpoints endpoint : endpointsList) {
                                List<Pod> backendPods = new ArrayList<>();
                                List<String> podNames = endpoint.getSubsets().stream()
                                        .flatMap(endpointSubset -> endpointSubset.getAddresses().stream())
                                        .map(address -> address.getTargetRef().getName()).collect(Collectors.toList());
                                backendPods.addAll(podNames.stream()
                                        .map(name -> client.pods().inNamespace(namespace).withName(name))
                                        .map(podPodResource -> podPodResource.get()).collect(Collectors.toList()));
                                items.put(endpoint, backendPods);
                            }
                        }
                        future.complete(items);
                    }, result -> {
                        if (result.succeeded()) {
                            @SuppressWarnings("unchecked")
                            Map<Endpoints, List<Pod>> endpoints = (Map<Endpoints, List<Pod>>) result.result();
                            emitter.complete(endpoints);
                        } else {
                            LOGGER.error("Unable to retrieve the endpoint from the {} service", application,
                                    result.cause());
                            emitter.fail(result.cause());
                        }
                    });
                });
        return endpointsUni.onItem().transform(endpoints -> toStorkServiceInstances(endpoints, previousInstances))
                .invoke(() -> invalidated.set(false));
    }

    private List<ServiceInstance> toStorkServiceInstances(Map<Endpoints, List<Pod>> backend,
            List<ServiceInstance> previousInstances) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (Map.Entry<Endpoints, List<Pod>> entry : backend.entrySet()) {
            Endpoints endPoints = entry.getKey();
            List<Pod> pods = entry.getValue();
            for (EndpointSubset subset : endPoints.getSubsets()) {
                for (EndpointAddress endpointAddress : subset.getAddresses()) {
                    String podName = endpointAddress.getTargetRef().getName();
                    String hostname = endpointAddress.getIp();
                    if (hostname == null) { // should we take the hostName?
                        hostname = endpointAddress.getHostname();
                    }
                    List<EndpointPort> endpointPorts = subset.getPorts();
                    Integer port = 0;
                    String protocol = "";
                    if (endpointPorts.size() == 1) {
                        port = endpointPorts.get(0).getPort();
                        protocol = endpointPorts.get(0).getProtocol();
                    } else {
                        for (EndpointPort endpointPort : endpointPorts) {
                            // return first endpoint port or the matching one with the provided port name.
                            if (portName == null || portName.equals(endpointPort.getName())) {
                                port = endpointPort.getPort();
                                protocol = endpointPort.getProtocol();
                                break;
                            }
                        }
                    }

                    ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, hostname, port);
                    if (matching != null) {
                        serviceInstances.add(matching);
                    } else {
                        Map<String, String> labels = new HashMap<>(endPoints.getMetadata().getLabels() != null
                                ? endPoints.getMetadata().getLabels()
                                : Collections.emptyMap());
                        Optional<Pod> maybePod = pods.stream().filter(pod -> pod.getMetadata().getName().equals(podName))
                                .findFirst();
                        String podNamespace = namespace;
                        if (maybePod.isPresent()) {
                            Pod pod = maybePod.get();
                            ObjectMeta metadata = pod.getMetadata();
                            podNamespace = metadata.getNamespace();
                            Map<String, String> podLabels = metadata.getLabels();
                            for (Map.Entry<String, String> label : podLabels.entrySet()) {
                                labels.putIfAbsent(label.getKey(), label.getValue());
                            }
                        }
                        //TODO add some useful metadata?
                        Metadata<KubernetesMetadataKey> k8sMetadata = Metadata.of(KubernetesMetadataKey.class);
                        serviceInstances.add(
                                new DefaultServiceInstance(ServiceInstanceIds.next(), hostname, port, Optional.empty(), secure,
                                        labels,
                                        k8sMetadata.with(META_K8S_SERVICE_ID, hostname).with(META_K8S_NAMESPACE, podNamespace)
                                                .with(META_K8S_PORT_PROTOCOL, protocol)));
                    }
                }
            }

        }
        return serviceInstances;
    }

    private static boolean isSecure(KubernetesConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }

}
