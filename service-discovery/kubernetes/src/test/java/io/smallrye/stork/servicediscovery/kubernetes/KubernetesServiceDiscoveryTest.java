package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.common.constraint.Assert;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryTest {

    KubernetesClient client;

    KubernetesMockServer server;

    String k8sMasterUrl;
    String defaultNamespace;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        defaultNamespace = client.getNamespace();
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        registerKubernetesResources(serviceName, defaultNamespace, ips);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespaceUsingProgrammaticAPI() {
        Stork stork = StorkTestUtils.getNewStorkInstance();
        stork.defineIfAbsent("svc", ServiceDefinition.of(
                new KubernetesConfiguration().withK8sHost(k8sMasterUrl).withK8sNamespace(defaultNamespace)));

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        registerKubernetesResources(serviceName, defaultNamespace, ips);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldHandleSecureAttribute() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "secure", "true"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        registerKubernetesResources(serviceName, defaultNamespace, ips);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isTrue());
    }

    @Test
    void shouldDiscoverServiceWithSpecificName() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "application", "rest-service"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesResources("rest-service", defaultNamespace, "10.96.96.231", "10.96.96.232",
                "10.96.96.233");
        registerKubernetesResources("svc", defaultNamespace, "10.95.95.125");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "rest-service"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
    }

    @Test
    void shouldGetServiceFromSpecificNamespace() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String specificNs = "ns1";

        registerKubernetesResources(serviceName, specificNs, "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
        instances.get().stream().map(ServiceInstance::getLabels)
                .forEach(serviceInstanceLabels -> assertThat(serviceInstanceLabels)
                        .contains(entry("app.kubernetes.io/name", "svc"), entry("app.kubernetes.io/version", "1.0")));
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
    }

    @Test
    void shouldGetServiceFromAllNamespace() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all"));
        Stork stork = StorkTestUtils.getNewStorkInstance();
        String serviceName = "svc";

        registerKubernetesResources(serviceName, "ns1", "10.96.96.231", "10.96.96.232", "10.96.96.233");
        registerKubernetesResources(serviceName, "ns2", "10.99.99.241", "10.99.99.242", "10.99.99.243");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(6);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233", "10.99.99.241", "10.99.99.242", "10.99.99.243");
        instances.get().stream().map(ServiceInstance::getLabels)
                .forEach(serviceInstanceLabels -> assertThat(serviceInstanceLabels)
                        .contains(entry("app.kubernetes.io/name", "svc"), entry("app.kubernetes.io/version", "1.0")));
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
                    entry("app.kubernetes.io/version", "1.0"),
                    entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
        }
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        Map<String, Long> idsByHostname = mapHostnameToIds(instances.get());

        client.endpoints().withName(serviceName).delete();
        client.pods().withName("svc-109696231").delete();
        client.pods().withName("svc-109696232").delete();
        client.pods().withName("svc-109696233").delete();

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232");

        Thread.sleep(5000);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(instances::get, Matchers.hasSize(2));

        for (ServiceInstance serviceInstance : instances.get()) {
            assertThat(idsByHostname.get(serviceInstance.getHost())).isEqualTo(serviceInstance.getId());
        }

        client.endpoints().withName(serviceName).delete();
        client.pods().withName("svc-109696231").delete();
        client.pods().withName("svc-109696232").delete();

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.234");

        Thread.sleep(5000);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(instances::get, Matchers.hasSize(3));

        for (ServiceInstance serviceInstance : instances.get()) {
            if (serviceInstance.getHost().equals("10.96.96.234")) {
                assertThat(idsByHostname.containsValue(serviceInstance.getId())).isFalse();
            } else {
                assertThat(idsByHostname.get(serviceInstance.getHost())).isEqualTo(serviceInstance.getId());
            }
        }
    }

    @Test
    void shouldFetchInstancesFromTheClusterWhenCacheIsInvalidated() throws InterruptedException {

        // Given a service with 3 instances registered in the cluster
        // Stork gather the cache from the cluster
        // When the endpoints are removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances : it gets 0 of them

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        client.endpoints().withName(serviceName).delete();

        Thread.sleep(5000);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get().isEmpty());

        assertThat(instances.get()).hasSize(0);
    }

    @Test
    void shouldFetchInstancesFromTheCache() throws InterruptedException {

        // Given an endpoint registered in the cluster
        // Stork gather the cache from the cluster
        // When an expectation is configured to throw an Error the next time we contact the cluster to get the endpoints and
        // Stork is called to get service instances
        // Stork get the instances from the cache: the error is not thrown because the cluster is not contacted.

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231");

        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReturn(HttpURLConnection.HTTP_INTERNAL_ERROR, "{}").once();

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231");
    }

    @Test
    void shouldGetInstancesFromTheCluster() throws InterruptedException {

        // Given an endpoint registered in the cluster
        // Stork gather the cache from the cluster
        // When an expectation in the cluster is configured to throw an Error the next time we try to get the endpoints and
        // When the endpoint is removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork gets the instances from the cache: the error is not thrown because the cluster is not contacted.

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231");

        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReturn(HttpURLConnection.HTTP_INTERNAL_ERROR, "{}").once();

        client.endpoints().withName(serviceName).delete();

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        Assertions.assertThrows(ConditionTimeoutException.class,
                () -> await()
                        .atMost(Duration.ofSeconds(5))
                        .until(() -> instances.get().isEmpty()));

    }

    private void registerKubernetesResources(String serviceName, String namespace, String... ips) {
        Assert.checkNotNullParam("ips", ips);
        buildAndRegisterKubernetesService(serviceName, namespace, true, ips);
        Arrays.stream(ips).map(ip -> buildAndRegisterBackendPod(serviceName, namespace, true, ip)).collect(Collectors.toList());
    }

    private Map<String, Long> mapHostnameToIds(List<ServiceInstance> serviceInstances) {
        Map<String, Long> result = new HashMap<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.put(serviceInstance.getHost(), serviceInstance.getId());
        }
        return result;
    }

    private Endpoints buildAndRegisterKubernetesService(String serviceName, String namespace, boolean register,
            String... ipAdresses) {

        Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put("app.kubernetes.io/name", serviceName);
        serviceLabels.put("app.kubernetes.io/version", "1.0");

        List<EndpointAddress> endpointAddresses = Arrays.stream(ipAdresses)
                .map(ipAddress -> {
                    ObjectReference targetRef = new ObjectReference(null, null, "Pod",
                            serviceName + "-" + ipAsSuffix(ipAddress), namespace, null, UUID.randomUUID().toString());
                    EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(ipAddress).withTargetRef(targetRef)
                            .build();
                    return endpointAddress;
                }).collect(Collectors.toList());
        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName(serviceName).withLabels(serviceLabels).endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddresses)
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        if (register) {
            if (namespace != null) {
                client.endpoints().inNamespace(namespace).resource(endpoint).create();
            } else {
                client.endpoints().resource(endpoint).create();
            }
        }
        return endpoint;

    }

    private Pod buildAndRegisterBackendPod(String name, String namespace, boolean register, String ip) {

        Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put("app.kubernetes.io/name", name);
        serviceLabels.put("app.kubernetes.io/version", "1.0");

        Map<String, String> podLabels = new HashMap<>(serviceLabels);
        podLabels.put("ui", "ui-" + ipAsSuffix(ip));
        Pod backendPod = new PodBuilder().withNewMetadata().withName(name + "-" + ipAsSuffix(ip))
                .withLabels(podLabels)
                .withNamespace(namespace)
                .endMetadata()
                .build();
        if (register) {
            if (namespace != null) {
                client.pods().inNamespace(namespace).resource(backendPod).create();
            } else {
                client.pods().resource(backendPod).create();
            }
        }
        return backendPod;
    }

    private String ipAsSuffix(String ipAddress) {
        return ipAddress.replace(".", "");
    }

}
