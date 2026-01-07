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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.discovery.v1.Endpoint;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSlice;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSliceList;
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
 *
 * Experimental feature: EndpointSlice-based service discovery.
 * Behavior and configuration may change in future versions.
 *
 */
public class KubernetesServiceDiscovery extends CachingServiceDiscovery {

    static final String METADATA_NAME = "metadata.name";
    public static final String DISCOVERY_K8S_API = "discovery.k8s.io";
    public static final String SERVICE_SELECTOR = "kubernetes.io/service-name";
    private final KubernetesClient client;
    private final String application;
    private final String portName;
    private final boolean allNamespaces;
    private final String namespace;
    private final boolean secure;
    private final Vertx vertx;
    private final int requestRetryBackoffLimit;
    private final int requestRetryBackoffInterval;
    private final boolean useEndpointSlices;

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
        this.requestRetryBackoffInterval = config.getRequestRetryBackoffInterval() == null ? 0
                : Integer.parseInt(config.getRequestRetryBackoffInterval());
        this.requestRetryBackoffLimit = config.getRequestRetryBackoffLimit() == null ? 0
                : Integer.parseInt(config.getRequestRetryBackoffLimit());

        allNamespaces = namespace != null && namespace.equalsIgnoreCase("all");

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace is not configured for service '" + serviceName
                    + "'. Please provide a namespace. Use 'all' to discover services in all namespaces");
        }

        Config k8sConfig = new ConfigBuilder(base)
                .withMasterUrl(masterUrl)
                .withRequestRetryBackoffLimit(requestRetryBackoffLimit)
                .withRequestRetryBackoffInterval(requestRetryBackoffInterval)
                .withNamespace(namespace).build();
        this.client = new KubernetesClientBuilder().withConfig(k8sConfig).build();
        this.vertx = vertx;
        this.secure = isSecure(config);
        this.useEndpointSlices = config.getUseEndpointSlices() == null ? Boolean.FALSE
                : Boolean.valueOf(config.getUseEndpointSlices());
        if (shouldUseEndpointSlices()) {
            configureSlicesInformer();
        } else {
            configureEndpointsInformer();
        }
    }

    /**
     * Returns whether service discovery should use EndpointSlices instead of classic Endpoints.
     *
     * <p>
     * Order of precedence:
     * </p>
     * <ol>
     * <li>User config: if {@code use-endpoint-slices} is explicitly set, that value is used.</li>
     * <li>If not set, use EndpointSlices only when the API is available and the service has slices.</li>
     * <li>Otherwise fall back to classic Endpoints.</li>
     * </ol>
     *
     * @return true if EndpointSlices should be used, false otherwise.
     */

    private boolean shouldUseEndpointSlices() {
        boolean shouldUseEndpointSlices = false;
        if (useEndpointSlices) {
            shouldUseEndpointSlices = true;
        }
        //cluster autodetection: EndpointSlices live in `discovery.k8s.io/v1`
        boolean apiAvailable = client.getApiGroups().getGroups().stream()
                .anyMatch(g -> DISCOVERY_K8S_API.equals(g.getName()));

        if (!apiAvailable) {
            shouldUseEndpointSlices = false; // old cluster - endpoints
        }

        EndpointSliceList slices = client.discovery().v1().endpointSlices()
                .inNamespace(namespace)
                .withLabel(SERVICE_SELECTOR, application)
                .list();
        shouldUseEndpointSlices = slices != null && !slices.getItems().isEmpty();
        if (shouldUseEndpointSlices) {
            LOGGER.info("EndpointSlice discovery is enabled (experimental)");
        }
        return shouldUseEndpointSlices;
    }

    private void configureEndpointsInformer() {
        configureInformer(
                ignore -> client.endpoints().inAnyNamespace(),
                ns -> client.endpoints().inNamespace(ns),
                Endpoints.class);
    }

    private void configureSlicesInformer() {
        configureInformer(
                ignore -> client.discovery().v1().endpointSlices().inAnyNamespace(),
                ns -> client.discovery().v1().endpointSlices().inNamespace(ns),
                EndpointSlice.class);
    }

    private <T> void configureInformer(
            Function<Boolean, AnyNamespaceOperation<T, ?, Resource<T>>> opAllNamespaces,
            Function<String, AnyNamespaceOperation<T, ?, Resource<T>>> opNamespace,
            Class<T> type) {
        AnyNamespaceOperation<T, ?, Resource<T>> op;
        if (allNamespaces) {
            op = opAllNamespaces.apply(true);
        } else {
            op = opNamespace.apply(namespace);
        }

        op.inform(new ResourceEventHandler<T>() {
            @Override
            public void onAdd(T obj) {
                LOGGER.info("{} added: {}", type.getSimpleName(), getName(obj));
                invalidate();
            }

            @Override
            public void onUpdate(T oldObj, T newObj) {
                LOGGER.info("{} updated: {}", type.getSimpleName(), getName(newObj));
                invalidate();
            }

            @Override
            public void onDelete(T obj, boolean deletedFinalStateUnknown) {
                LOGGER.info("{} deleted: {}", type.getSimpleName(), getName(obj));
                invalidate();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> String getName(T obj) {
        return ((HasMetadata) obj).getMetadata().getName();
    }

    @Override
    public Uni<List<ServiceInstance>> cache(Uni<List<ServiceInstance>> uni) {
        return uni.memoize().until(() -> invalidated.get());
    }

    @Override
    public void invalidate() {
        invalidated.set(true);
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        if (shouldUseEndpointSlices()) {
            Uni<List<EndpointSlice>> endpointSlices = fetchServiceEndpointSlice();
            return endpointSlices.onItem().transform(slices -> fromSlicesToStorkServiceInstances(slices, previousInstances))
                    .invoke(() -> invalidated.set(false));
        } else {
            Uni<Map<Endpoints, List<Pod>>> endpointsUni = fetchServiceEnpoints();
            return endpointsUni.onItem()
                    .transform(endpoints -> fromEndpointsToStorkServiceInstances(endpoints, previousInstances))
                    .invoke(() -> invalidated.set(false));
        }

    }

    private Uni<Map<Endpoints, List<Pod>>> fetchServiceEnpoints() {
        Uni<Map<Endpoints, List<Pod>>> endpointsUni = Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(future -> {
                        final Map<Endpoints, List<Pod>> items;

                        if (allNamespaces) {
                            List<Endpoints> endpointsList = client.endpoints().inAnyNamespace()
                                    .withField(METADATA_NAME, application).list()
                                    .getItems();
                            items = gatherBackendPodsInAnyNamespace(endpointsList);
                        } else {
                            List<Endpoints> endpointsList = client.endpoints().inNamespace(namespace)
                                    .withField(METADATA_NAME, application)
                                    .list()
                                    .getItems();
                            items = gatherBackendPodsInNamespace(endpointsList);
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
        return endpointsUni;
    }

    private Uni<List<EndpointSlice>> fetchServiceEndpointSlice() {
        Uni<List<EndpointSlice>> slicesUni = Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(future -> {
                        List<EndpointSlice> items = new ArrayList<>();

                        if (allNamespaces) {
                            List<EndpointSlice> endpointSlices = client.discovery().v1().endpointSlices()
                                    .inNamespace(namespace)
                                    .withLabel(SERVICE_SELECTOR, application)
                                    .list().getItems();
                            items.addAll(endpointSlices);

                        } else {
                            List<EndpointSlice> endpointSlices = client.discovery().v1().endpointSlices().inNamespace(namespace)
                                    .withLabel(SERVICE_SELECTOR, application)
                                    .list()
                                    .getItems();
                            items.addAll(endpointSlices);
                        }
                        future.complete(items);
                    }, result -> {
                        if (result.succeeded()) {
                            @SuppressWarnings("unchecked")
                            List<EndpointSlice> endpointSlices = (List<EndpointSlice>) result.result();
                            emitter.complete(endpointSlices);
                        } else {
                            LOGGER.error("Unable to retrieve the endpoint from the {} service", application,
                                    result.cause());
                            emitter.fail(result.cause());
                        }
                    });
                });
        return slicesUni;
    }

    private List<ServiceInstance> fromSlicesToStorkServiceInstances(List<EndpointSlice> endpointSlices,
            List<ServiceInstance> previousInstances) {

        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (EndpointSlice slice : endpointSlices) {
            Map<String, String> labels = new HashMap<>(slice.getMetadata().getLabels() != null
                    ? slice.getMetadata().getLabels()
                    : Collections.emptyMap());
            List<io.fabric8.kubernetes.api.model.discovery.v1.EndpointPort> ports = slice.getPorts();
            for (Endpoint endpoint : slice.getEndpoints()) {

                if (endpoint.getConditions() != null
                        && Boolean.FALSE.equals(endpoint.getConditions().getReady())) {
                    continue;
                }

                for (String address : endpoint.getAddresses()) {
                    for (io.fabric8.kubernetes.api.model.discovery.v1.EndpointPort port : ports) {

                        ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, address,
                                port.getPort());
                        if (matching != null) {
                            serviceInstances.add(matching);
                        } else {
                            Metadata<KubernetesMetadataKey> k8sMetadata = Metadata.of(KubernetesMetadataKey.class);
                            DefaultServiceInstance serviceInstance = new DefaultServiceInstance(ServiceInstanceIds.next(),
                                    address, port.getPort(), Optional.empty(), secure,
                                    labels,
                                    k8sMetadata.with(META_K8S_SERVICE_ID, address).with(META_K8S_NAMESPACE, namespace));
                            serviceInstances.add(serviceInstance);
                        }
                    }
                }
            }
        }
        return serviceInstances;
    }

    private Map<Endpoints, List<Pod>> gatherBackendPodsInNamespace(List<Endpoints> endpointsList) {
        Map<Endpoints, List<Pod>> items = new HashMap<>();
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
        return items;
    }

    private Map<Endpoints, List<Pod>> gatherBackendPodsInAnyNamespace(List<Endpoints> endpointsList) {
        Map<Endpoints, List<Pod>> items = new HashMap<>();
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
        return items;
    }

    private List<ServiceInstance> fromEndpointsToStorkServiceInstances(Map<Endpoints, List<Pod>> backend,
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
