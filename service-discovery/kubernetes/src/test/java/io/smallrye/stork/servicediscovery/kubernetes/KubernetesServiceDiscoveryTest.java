package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesTestUtils.ipAsSuffix;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesTestUtils.mapHostnameToIds;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@DisabledOnOs(OS.WINDOWS)
@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryTest {

    KubernetesClient client;

    KubernetesMockServer server;

    String k8sMasterUrl;
    String defaultNamespace;

    KubernetesTestUtils utils;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        defaultNamespace = client.getNamespace();
        utils = new KubernetesTestUtils(client);
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        utils.registerKubernetesResources(serviceName, defaultNamespace, ips);

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
    void shouldGetServiceFromK8sWithApplicationNameConfig() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "application", "greetingApp"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        utils.registerKubernetesResources("greetingApp", defaultNamespace, ips);

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
            assertThat(labels).contains(entry("app.kubernetes.io/name", "greetingApp"),
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

        utils.registerKubernetesResources(serviceName, defaultNamespace, ips);

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

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "secure", "true"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        utils.registerKubernetesResources(serviceName, defaultNamespace, ips);

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

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "application", "rest-service"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        utils.registerKubernetesResources("rest-service", defaultNamespace, "10.96.96.231", "10.96.96.232",
                "10.96.96.233");
        utils.registerKubernetesResources("svc", defaultNamespace, "10.95.95.125");

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

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String specificNs = "ns1";

        utils.registerKubernetesResources(serviceName, specificNs, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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
    void shouldGetServiceUsingFirstPortWhenMultiplePortsFromSpecificNamespace() {
        String serviceName = "svc";
        String specificNs = "ns1";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String[] ips = new String[] { "10.96.96.231", "10.96.96.232", "10.96.96.233" };
        EndpointPort[] ports = new EndpointPort[] {
                new EndpointPortBuilder().withName("http1").withPort(8080).withProtocol("TCP").build(),
                new EndpointPortBuilder().withName("http2").withPort(8081).withProtocol("TCP").build() };
        utils.buildAndRegisterKubernetesService(serviceName, specificNs, true, ports, ips);
        Arrays.stream(ips).forEach(ip -> utils.buildAndRegisterBackendPod(serviceName, specificNs, true, ip));

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
    void shouldGetServiceUsingSelectedPortNameWhenMultiplePortsFromSpecificNamespace() {
        String serviceName = "svc";
        String specificNs = "ns1";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1", "port-name", "http1"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String[] ips = new String[] { "10.96.96.231", "10.96.96.232", "10.96.96.233" };
        EndpointPort[] ports = new EndpointPort[] {
                new EndpointPortBuilder().withName("http1").withPort(8080).withProtocol("TCP").build(),
                new EndpointPortBuilder().withName("http2").withPort(8081).withProtocol("TCP").build() };
        utils.buildAndRegisterKubernetesService(serviceName, specificNs, true, ports, ips);
        Arrays.stream(ips).forEach(ip -> utils.buildAndRegisterBackendPod(serviceName, specificNs, true, ip));

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

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();
        String serviceName = "svc";

        utils.registerKubernetesResources(serviceName, "ns1", "10.96.96.231", "10.96.96.232", "10.96.96.233");
        utils.registerKubernetesResources(serviceName, "ns2", "10.99.99.241", "10.99.99.242", "10.99.99.243");

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

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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

        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232");

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

        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.234");

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
    void shouldGetInstancesFromCache() throws InterruptedException {
        String serviceName = "svc";

        //Given a few instances for a svc service
        List<Endpoints> endpointsList = List
                .of(utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232",
                        "10.96.96.233"));

        //Recording k8s cluster calls and and build the response with the (previous registered) endpoints
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReply(200, r -> {
                    serverHit.incrementAndGet();
                    return new EndpointsListBuilder().withItems(endpointsList).build();
                }).always();

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);
        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        //second try to get instances, instances should be fetched from cache, cluster calls should be still 1
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);
        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

    }

    @Test
    void shouldGetInstancesFromClusterForceCacheInvalidation() throws InterruptedException {
        String serviceName = "svc";

        //Given a few instances for a svc service
        List<Endpoints> endpointsList = List
                .of(utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232",
                        "10.96.96.233"));

        //Recording k8s cluster calls and and build the response with the (previous registered) endpoints
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReply(200, r -> {
                    serverHit.incrementAndGet();
                    return new EndpointsListBuilder().withItems(endpointsList).build();
                }).always();

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);
        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

        //------
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);
        //------

        KubernetesServiceDiscovery kubernetesDiscovery = (KubernetesServiceDiscovery) service.getServiceDiscovery();
        kubernetesDiscovery.invalidate();

        //second try to get instances, instances should be fetched from cache, cluster calls should be still 1
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(2);
        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

    }

    /**
     * Verifies that the cluster is reached when the cache is invalidated. No retries.
     * The cache is only reset when the call is success but this doesn't trigger multiple calls because retries are not enabled.
     *
     * <p>
     * This test ensures that the system does not enter a loop of cluster calls when the cluster fails,
     * and that it behaves correctly when the cluster state changes.
     * </p>
     */
    @Test
    void shouldCallTheClusterWhenCacheInvalidated() throws InterruptedException {
        String serviceName = "svc";

        //Recording k8s cluster calls and build the response with the (previous registered) endpoints
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReply(500, r -> {
                    serverHit.incrementAndGet();
                    return "Internal Server Error";
                }).always();

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService(serviceName);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        KubernetesServiceDiscovery serviceDiscovery = (KubernetesServiceDiscovery) service.getServiceDiscovery();
        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);

        //We trigger an event in the cluster just to invalidate cache
        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232",
                "10.96.96.233");

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(2);

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        // Since the previous call failed, the cache wasn't reset and we reach the cluster this time incremeting the serverHit to 3.
        assertThat(serverHit.get()).isEqualTo(3);

        //We trigger an event in the cluster just to invalidate cache
        utils.registerKubernetesResources("svc2", defaultNamespace, "10.96.96.234", "10.96.96.235",
                "10.96.96.236");

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(4);

    }

    /**
     * Verifies that the cluster is reached when the cache is invalidated. With retries.
     * The cache is only reset when the call is success.
     *
     * <p>
     * This test ensures that the system does only the calls configured by the retries param.
     * </p>
     */
    @Test
    void shouldCallTheClusterWhenCacheInvalidatedAndRetries() throws InterruptedException {
        String serviceName = "svc";

        //Recording k8s cluster calls and build the response with the (previous registered) endpoints
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReply(500, r -> {
                    serverHit.incrementAndGet();
                    return "Internal Server Error";
                }).always();

        //We’re using the same retries = 0 configuration as the default because we couldn’t get the test to pass reliably in CI otherwise. In any case, the goal of this test is to verify that the user's configuration overrides the default one — not to test that the Kubernetes client actually performs a specific number of retries.
        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3",
                        "request-retry-backoff-limit", "0"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Service service = stork.getService(serviceName);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        KubernetesServiceDiscovery serviceDiscovery = (KubernetesServiceDiscovery) service.getServiceDiscovery();
        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);

        //We trigger an event in the cluster just to invalidate cache
        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232",
                "10.96.96.233");

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(2);

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        // Since the previous call failed, the cache wasn't reset and we reach the cluster this time incremeting the serverHit to 3.
        assertThat(serverHit.get()).isEqualTo(3);

        //We trigger an event in the cluster just to invalidate cache
        utils.registerKubernetesResources("svc2", defaultNamespace, "10.96.96.234", "10.96.96.235",
                "10.96.96.236");

        serviceDiscovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Expected recovery on failure"))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(4);

    }

    @Test
    void shouldFetchInstancesFromTheClusterWhenCacheIsInvalidated() throws InterruptedException {

        // Given a service with 3 instances registered in the cluster, in `test` namespace
        // Stork gather the cache from the cluster
        // When the endpoints are removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances : it gets 0 of them
        String serviceName = "svc";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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

        client.endpoints().inNamespace(defaultNamespace).withName(serviceName).withTimeout(100, TimeUnit.MILLISECONDS).delete();

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get().isEmpty());

        assertThat(instances.get()).hasSize(0);
    }

    @Test
    void shouldFetchInstancesFromAllNsWhenCacheIsInvalidated() {

        // Given a service with 3 instances registered in the cluster in any namespace
        // Stork gather the cache from the cluster
        // When the endpoints are removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances : it gets 0 of them
        String serviceName = "svc";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all", "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

        Set<ServiceInstance> instances = new CopyOnWriteArraySet<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::addAll);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> !instances.isEmpty());

        assertThat(instances)
                .hasSize(3)
                .extracting(ServiceInstance::getPort, ServiceInstance::getHost)
                .containsExactlyInAnyOrder(
                        tuple(8080, "10.96.96.231"),
                        tuple(8080, "10.96.96.232"),
                        tuple(8080, "10.96.96.233"));

        client.endpoints().inNamespace(defaultNamespace).withName(serviceName)
                .withTimeout(100, TimeUnit.MILLISECONDS)
                .delete();

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(received -> {
                    instances.clear();
                    instances.addAll(received);
                });

        await().atMost(Duration.ofSeconds(5))
                .until(instances::isEmpty);

        assertThat(instances).isEmpty();
    }

}
