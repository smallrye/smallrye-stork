package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
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
import io.smallrye.stork.test.TestConfigProviderBean;

@DisabledOnOs(OS.WINDOWS)
@ExtendWith(WeldJunit5Extension.class)
@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            KubernetesServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    KubernetesClient client;

    KubernetesMockServer server;

    String k8sMasterUrl;
    String defaultNamespace;

    @BeforeEach
    void setUp() {
        config.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        defaultNamespace = client.getNamespace();
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() {
        config.addServiceConfig("svc", null, "kubernetes",
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
    void shouldGetServiceFromK8sWithApplicationNameConfig() {
        config.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "application", "greetingApp"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        registerKubernetesResources("greetingApp", defaultNamespace, ips);

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

        config.addServiceConfig("svc", null, "kubernetes",
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

        config.addServiceConfig("svc", null, "kubernetes",
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

        config.addServiceConfig("svc", null, "kubernetes",
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
    void shouldGetServiceUsingFirstPortWhenMultiplePortsFromSpecificNamespace() {
        String serviceName = "svc";
        String specificNs = "ns1";

        config.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String[] ips = new String[] { "10.96.96.231", "10.96.96.232", "10.96.96.233" };
        EndpointPort[] ports = new EndpointPort[] {
                new EndpointPortBuilder().withName("http1").withPort(8080).withProtocol("TCP").build(),
                new EndpointPortBuilder().withName("http2").withPort(8081).withProtocol("TCP").build() };
        buildAndRegisterKubernetesService(serviceName, specificNs, true, ports, ips);
        Arrays.stream(ips).forEach(ip -> buildAndRegisterBackendPod(serviceName, specificNs, true, ip));

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

        config.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1", "port-name", "http1"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String[] ips = new String[] { "10.96.96.231", "10.96.96.232", "10.96.96.233" };
        EndpointPort[] ports = new EndpointPort[] {
                new EndpointPortBuilder().withName("http1").withPort(8080).withProtocol("TCP").build(),
                new EndpointPortBuilder().withName("http2").withPort(8081).withProtocol("TCP").build() };
        buildAndRegisterKubernetesService(serviceName, specificNs, true, ports, ips);
        Arrays.stream(ips).forEach(ip -> buildAndRegisterBackendPod(serviceName, specificNs, true, ip));

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

        config.addServiceConfig("svc", null, "kubernetes",
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

        config.addServiceConfig("svc", null, "kubernetes",
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
    void shouldGetInstancesFromCache() throws InterruptedException {
        String serviceName = "svc";

        //Recording k8s cluster calls and build a endpoints as response
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
                .andReply(200, r -> {
                    serverHit.incrementAndGet();
                    List<Endpoints> endpointsList = new ArrayList<>();
                    endpointsList.add(registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232",
                            "10.96.96.233"));
                    return new EndpointsListBuilder().withItems(endpointsList).build();
                }).always();

        config.addServiceConfig(serviceName, null, "kubernetes", null,
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
    void shouldFetchInstancesFromTheClusterWhenCacheIsInvalidated() throws InterruptedException {

        // Given a service with 3 instances registered in the cluster, in `test` namespace
        // Stork gather the cache from the cluster
        // When the endpoints are removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances : it gets 0 of them
        String serviceName = "svc";

        config.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

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

        client.endpoints().inNamespace(defaultNamespace).withName(serviceName).delete();

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

        config.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all", "refresh-period", "3"), null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.233");

        Set<ServiceInstance> instances = new ConcurrentHashSet<>();

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

    private Endpoints registerKubernetesResources(String serviceName, String namespace, String... ips) {
        Assert.checkNotNullParam("ips", ips);
        Endpoints endpoints = buildAndRegisterKubernetesService(serviceName, namespace, true, ips);
        Arrays.stream(ips).forEach(ip -> buildAndRegisterBackendPod(serviceName, namespace, true, ip));
        return endpoints;
    }

    private Map<String, Long> mapHostnameToIds(List<ServiceInstance> serviceInstances) {
        Map<String, Long> result = new HashMap<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.put(serviceInstance.getHost(), serviceInstance.getId());
        }
        return result;
    }

    private Endpoints buildAndRegisterKubernetesService(String applicationName, String namespace, boolean register,
            String... ipAdresses) {
        EndpointPort[] ports = new EndpointPort[] { new EndpointPortBuilder().withPort(8080).withProtocol("TCP").build() };
        return buildAndRegisterKubernetesService(applicationName, namespace, register, ports, ipAdresses);
    }

    private Endpoints buildAndRegisterKubernetesService(String applicationName, String namespace, boolean register,
            EndpointPort[] ports, String... ipAdresses) {

        Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put("app.kubernetes.io/name", applicationName);
        serviceLabels.put("app.kubernetes.io/version", "1.0");

        List<EndpointAddress> endpointAddresses = Arrays.stream(ipAdresses)
                .map(ipAddress -> {
                    ObjectReference targetRef = new ObjectReference(null, null, "Pod",
                            applicationName + "-" + ipAsSuffix(ipAddress), namespace, null, UUID.randomUUID().toString());
                    EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(ipAddress).withTargetRef(targetRef)
                            .build();
                    return endpointAddress;
                }).collect(Collectors.toList());
        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName(applicationName).withLabels(serviceLabels).endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddresses)
                        .addToPorts(ports)
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
