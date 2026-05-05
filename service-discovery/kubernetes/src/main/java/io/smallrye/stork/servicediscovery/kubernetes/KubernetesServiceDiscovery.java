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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.discovery.v1.Endpoint;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSlice;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Gettable;
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
 * <p>
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
    private final Boolean useEndpointSlices;
    private final boolean useEndpointSlicesEnabled;
    private final boolean useClusterIp;

    private static final Logger LOGGER = Logger.getLogger(KubernetesServiceDiscovery.class);

    private final AtomicBoolean invalidated = new AtomicBoolean();

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
        int requestRetryBackoffInterval = config.getRequestRetryBackoffInterval() == null ? 0
                : Integer.parseInt(config.getRequestRetryBackoffInterval());
        int requestRetryBackoffLimit = config.getRequestRetryBackoffLimit() == null ? 0
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
        this.useEndpointSlices = config.getUseEndpointSlices() == null ? null
                : Boolean.valueOf(config.getUseEndpointSlices());
        this.useEndpointSlicesEnabled = shouldUseEndpointSlices();
        this.useClusterIp = config.getUseClusterIp() != null && Boolean.parseBoolean(config.getUseClusterIp());
        if (useClusterIp && Boolean.TRUE.equals(useEndpointSlices)) {
            LOGGER.warnf("Both 'use-cluster-ip' and 'use-endpoint-slices' are enabled for service '%s'. "
                    + "'use-cluster-ip' takes precedence; 'use-endpoint-slices' will be ignored.", serviceName);
        }
        if (useClusterIp) {
            configureServicesInformer();
        } else if (useEndpointSlicesEnabled) {
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
     * <li>If not set, use EndpointSlices only when the API is available.</li>
     * <li>Otherwise fall back to classic Endpoints.</li>
     * </ol>
     *
     * @return true if EndpointSlices should be used, false otherwise.
     */

    private boolean shouldUseEndpointSlices() {
        // if disabled explicitly, do not use autodetection
        if (Boolean.FALSE.equals(useEndpointSlices)) {
            return false; // old cluster - endpoints
        }
        // cluster autodetection: EndpointSlices live in `discovery.k8s.io/v1`
        // use autodetection even if explicitly set to true, to avoid misconfiguration on clusters that do not support EndpointSlices
        boolean apiAvailable = client.getApiGroups().getGroups().stream()
                .anyMatch(g -> DISCOVERY_K8S_API.equals(g.getName()));

        if (!apiAvailable) {
            return false; // old cluster - endpoints
        }

        EndpointSliceList slices = client.discovery().v1().endpointSlices()
                .inNamespace(namespace)
                .withLabel(SERVICE_SELECTOR, application)
                .list();
        boolean shouldUseEndpointSlices = slices != null && !slices.getItems().isEmpty();
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

    private void configureServicesInformer() {
        configureInformer(
                ignore -> client.services().inAnyNamespace(),
                ns -> client.services().inNamespace(ns),
                Service.class);
    }

    /**
     * Sets up a Kubernetes informer that invalidates the cache on resource changes.
     * Used for {@link Endpoints}, {@link EndpointSlice}, and {@link Service} resources.
     * The {@code ? extends Resource<T>} bounds are needed because fabric8 resource operations
     * use different {@link Resource} subtypes (e.g. {@code ServiceResource<Service>}).
     *
     * @param <T> the Kubernetes resource type
     * @param opAllNamespaces supplier for the all-namespaces operation
     * @param opNamespace supplier for a single-namespace operation
     * @param type the resource class, used for log messages
     */
    private <T> void configureInformer(
            Function<Boolean, ? extends AnyNamespaceOperation<T, ?, ? extends Resource<T>>> opAllNamespaces,
            Function<String, ? extends AnyNamespaceOperation<T, ?, ? extends Resource<T>>> opNamespace,
            Class<T> type) {
        AnyNamespaceOperation<T, ?, ? extends Resource<T>> op;
        if (allNamespaces) {
            op = opAllNamespaces.apply(true);
        } else {
            op = opNamespace.apply(namespace);
        }

        op.inform(new ResourceEventHandler<T>() {
            @Override
            public void onAdd(T obj) {
                LOGGER.infof("%s added: %s", type.getSimpleName(), getName(obj));
                invalidate();
            }

            @Override
            public void onUpdate(T oldObj, T newObj) {
                LOGGER.infof("%s updated: %s", type.getSimpleName(), getName(newObj));
                invalidate();
            }

            @Override
            public void onDelete(T obj, boolean deletedFinalStateUnknown) {
                LOGGER.infof("%s deleted: %s", type.getSimpleName(), getName(obj));
                invalidate();
            }
        });
    }

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
        Uni<List<ServiceInstance>> result;
        if (useClusterIp) {
            result = fetchServiceClusterIp().onItem()
                    .transform(clusterIps -> fromClusterIpServicesToStorkServiceInstances(clusterIps, previousInstances));
        } else if (useEndpointSlicesEnabled) {
            result = fetchServiceEndpointSlice().onItem()
                    .transform(slices -> fromSlicesToStorkServiceInstances(slices, previousInstances));
        } else {
            result = fetchServiceEndpoints().onItem()
                    .transform(endpoints -> fromEndpointsToStorkServiceInstances(endpoints, previousInstances));
        }
        return result
                .invoke(() -> invalidated.set(false));
    }

    private <T> Uni<T> executeOnWorkerThread(Supplier<T> supplier) {
        return Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(supplier::get).onComplete(result -> {
                        if (result.succeeded()) {
                            emitter.complete(result.result());
                        } else {
                            LOGGER.errorf("Unable to retrieve resources from the %s service", application,
                                    result.cause());
                            emitter.fail(result.cause());
                        }
                    });
                });
    }

    private Uni<Map<Endpoints, List<Pod>>> fetchServiceEndpoints() {
        return executeOnWorkerThread(() -> {
            if (allNamespaces) {
                List<Endpoints> endpointsList = client.endpoints().inAnyNamespace()
                        .withField(METADATA_NAME, application).list().getItems();
                return gatherBackendPodsInAnyNamespace(endpointsList);
            } else {
                List<Endpoints> endpointsList = client.endpoints().inNamespace(namespace)
                        .withField(METADATA_NAME, application).list().getItems();
                return gatherBackendPodsInNamespace(endpointsList);
            }
        });
    }

    private Uni<List<EndpointSlice>> fetchServiceEndpointSlice() {
        return executeOnWorkerThread(() -> {
            if (allNamespaces) {
                return client.discovery().v1().endpointSlices()
                        .inNamespace(namespace)
                        .withLabel(SERVICE_SELECTOR, application)
                        .list().getItems();
            } else {
                return client.discovery().v1().endpointSlices().inNamespace(namespace)
                        .withLabel(SERVICE_SELECTOR, application)
                        .list().getItems();
            }
        });
    }

    private Uni<List<Service>> fetchServiceClusterIp() {
        return executeOnWorkerThread(() -> {
            if (allNamespaces) {
                return client.services().inAnyNamespace()
                        .withField(METADATA_NAME, application).list().getItems();
            } else {
                Service svc = client.services()
                        .inNamespace(namespace).withName(application).get();
                return svc != null ? List.of(svc) : Collections.emptyList();
            }
        });
    }

    private List<ServiceInstance> fromClusterIpServicesToStorkServiceInstances(List<Service> clusterIpServices,
            List<ServiceInstance> previousInstances) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (Service instance : clusterIpServices) {
            List<ServicePort> servicePorts = instance.getSpec().getPorts();
            if (servicePorts == null || servicePorts.isEmpty()) {
                LOGGER.warnf("Skipping service '%s' in namespace '%s': no ports defined",
                        application, instance.getMetadata().getNamespace());
                continue;
            }
            ResolvedPort resolved = resolvePort(servicePorts,
                    ServicePort::getName, ServicePort::getPort, ServicePort::getProtocol);
            if (resolved == null) {
                LOGGER.warnf("Skipping service '%s' in namespace '%s': no matching port found for port-name '%s'",
                        application, instance.getMetadata().getNamespace(), portName);
                continue;
            }

            String clusterIp = instance.getSpec().getClusterIP();
            if (clusterIp == null || "None".equals(clusterIp)) {
                LOGGER.warnf("Skipping headless service '%s' in namespace '%s'",
                        application, instance.getMetadata().getNamespace());
                continue;
            }
            Map<String, String> labels = instance.getMetadata().getLabels() != null
                    ? new HashMap<>(instance.getMetadata().getLabels())
                    : new HashMap<>();
            serviceInstances.add(findOrCreateServiceInstance(previousInstances,
                    clusterIp, resolved.port(), labels, instance.getMetadata().getNamespace(), resolved.protocol()));

        }
        return serviceInstances;
    }

    private List<ServiceInstance> fromSlicesToStorkServiceInstances(List<EndpointSlice> endpointSlices,
            List<ServiceInstance> previousInstances) {

        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (EndpointSlice slice : endpointSlices) {
            Map<String, String> labels = new HashMap<>(slice.getMetadata().getLabels() != null
                    ? slice.getMetadata().getLabels()
                    : Collections.emptyMap());
            List<io.fabric8.kubernetes.api.model.discovery.v1.EndpointPort> ports = slice.getPorts();
            if (ports == null || ports.isEmpty()) {
                continue;
            }
            for (Endpoint endpoint : slice.getEndpoints()) {

                if (endpoint.getConditions() != null
                        && Boolean.FALSE.equals(endpoint.getConditions().getReady())) {
                    continue;
                }

                for (String address : endpoint.getAddresses()) {
                    for (io.fabric8.kubernetes.api.model.discovery.v1.EndpointPort port : ports) {
                        if (portName != null && !portName.equals(port.getName())) {
                            continue;
                        }

                        serviceInstances.add(findOrCreateServiceInstance(previousInstances,
                                address, port.getPort(), labels, namespace, null));
                    }
                }
            }
        }
        return serviceInstances;
    }

    private Map<Endpoints, List<Pod>> gatherBackendPodsInNamespace(List<Endpoints> endpointsList) {
        Map<Endpoints, List<Pod>> items = new HashMap<>();
        for (Endpoints endpoint : endpointsList) {
            List<String> podNames = endpoint.getSubsets().stream()
                    .flatMap(endpointSubset -> endpointSubset.getAddresses().stream())
                    .map(address -> address.getTargetRef().getName()).toList();
            List<Pod> backendPods = podNames.stream()
                    .map(name -> client.pods().inNamespace(namespace).withName(name))
                    .map(Gettable::get).collect(Collectors.toList());
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
                    .map(address -> address.getTargetRef().getName()).toList();
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
                    if (endpointPorts == null || endpointPorts.isEmpty()) {
                        continue;
                    }
                    ResolvedPort resolved = resolvePort(endpointPorts,
                            EndpointPort::getName, EndpointPort::getPort, EndpointPort::getProtocol);
                    if (resolved == null) {
                        LOGGER.warnf("Skipping endpoint for service '%s': no matching port found for port-name '%s'",
                                application, portName);
                        continue;
                    }

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
                    serviceInstances.add(findOrCreateServiceInstance(previousInstances,
                            hostname, resolved.port(), labels, podNamespace, resolved.protocol()));
                }
            }

        }
        return serviceInstances;
    }

    private record ResolvedPort(int port, String protocol) {
    }

    private <P> ResolvedPort resolvePort(List<P> ports,
            Function<P, String> getName, Function<P, Integer> getPort, Function<P, String> getProtocol) {
        if (ports.size() == 1) {
            return new ResolvedPort(getPort.apply(ports.get(0)), getProtocol.apply(ports.get(0)));
        }
        for (P p : ports) {
            if (portName == null || portName.equals(getName.apply(p))) {
                return new ResolvedPort(getPort.apply(p), getProtocol.apply(p));
            }
        }
        return null;
    }

    private ServiceInstance findOrCreateServiceInstance(List<ServiceInstance> previousInstances,
            String host, int port, Map<String, String> labels, String instanceNamespace, String protocol) {
        ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, host, port);
        if (matching != null) {
            return matching;
        }
        Metadata<KubernetesMetadataKey> k8sMetadata = Metadata.of(KubernetesMetadataKey.class)
                .with(META_K8S_SERVICE_ID, host)
                .with(META_K8S_NAMESPACE, instanceNamespace);
        if (protocol != null && !protocol.isEmpty()) {
            k8sMetadata = k8sMetadata.with(META_K8S_PORT_PROTOCOL, protocol);
        }
        return new DefaultServiceInstance(ServiceInstanceIds.next(),
                host, port, Optional.empty(), secure, labels, k8sMetadata);
    }

    private static boolean isSecure(KubernetesConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }

}
