package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_NAMESPACE;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_PORT_PROTOCOL;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesTestUtils.servicePort;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@DisabledOnOs(OS.WINDOWS)
@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryClusterIpTest {

    KubernetesClient client;

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
    void shouldResolveClusterIpWithSinglePort() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100",
                servicePort("http", 8080, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance si = instances.get().get(0);
        assertThat(si.getHost()).isEqualTo("10.0.0.100");
        assertThat(si.getPort()).isEqualTo(8080);
        assertThat(si.isSecure()).isFalse();
        assertThat(si.getLabels()).contains(
                entry("app.kubernetes.io/name", "svc"),
                entry("app.kubernetes.io/version", "1.0"));

        @SuppressWarnings("unchecked")
        Metadata<KubernetesMetadataKey> metadata = (Metadata<KubernetesMetadataKey>) si.getMetadata();
        assertThat(metadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        assertThat(metadata.getMetadata().get(META_K8S_SERVICE_ID)).isEqualTo("10.0.0.100");
        assertThat(metadata.getMetadata()).containsKey(META_K8S_NAMESPACE);
        assertThat(metadata.getMetadata().get(META_K8S_PORT_PROTOCOL)).isEqualTo("TCP");
    }

    @Test
    void shouldResolveClusterIpWithMultiplePortsUsingFirstPort() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100",
                servicePort("http", 8080, "TCP"),
                servicePort("grpc", 50051, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance si = instances.get().get(0);
        assertThat(si.getHost()).isEqualTo("10.0.0.100");
        assertThat(si.getPort()).isEqualTo(8080);
    }

    @Test
    void shouldResolveClusterIpWithMultiplePortsUsingPortName() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true", "port-name", "grpc"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100",
                servicePort("http", 8080, "TCP"),
                servicePort("grpc", 50051, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance si = instances.get().get(0);
        assertThat(si.getHost()).isEqualTo("10.0.0.100");
        assertThat(si.getPort()).isEqualTo(50051);
    }

    @Test
    void shouldResolveClusterIpWithSecureFlag() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true", "secure", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100",
                servicePort("https", 8443, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).isSecure()).isTrue();
        assertThat(instances.get().get(0).getPort()).isEqualTo(8443);
    }

    @Test
    void shouldResolveClusterIpWithApplicationNameOverride() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true", "application", "my-backend"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("my-backend", defaultNamespace, "10.0.0.200",
                servicePort("http", 9090, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance si = instances.get().get(0);
        assertThat(si.getHost()).isEqualTo("10.0.0.200");
        assertThat(si.getPort()).isEqualTo(9090);
        assertThat(si.getLabels()).contains(entry("app.kubernetes.io/name", "my-backend"));
    }

    @Test
    void shouldResolveClusterIpInSpecificNamespace() {
        String ns = "production";

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", ns,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", ns, "10.0.1.50",
                servicePort("http", 8080, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance si = instances.get().get(0);
        assertThat(si.getHost()).isEqualTo("10.0.1.50");
        assertThat(si.getPort()).isEqualTo(8080);

        @SuppressWarnings("unchecked")
        Metadata<KubernetesMetadataKey> metadata = (Metadata<KubernetesMetadataKey>) si.getMetadata();
        assertThat(metadata.getMetadata().get(META_K8S_NAMESPACE)).isEqualTo(ns);
    }

    @Test
    void shouldPreserveIdOnRefetchInClusterIpMode() throws InterruptedException {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true", "refresh-period", "3"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100",
                servicePort("http", 8080, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        long originalId = instances.get().get(0).getId();

        KubernetesServiceDiscovery sd = (KubernetesServiceDiscovery) service.getServiceDiscovery();
        sd.invalidate();

        instances.set(null);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getId()).isEqualTo(originalId);
        assertThat(instances.get().get(0).getHost()).isEqualTo("10.0.0.100");
    }

    @Test
    void shouldReturnEmptyListForHeadlessService() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "None",
                servicePort("http", 8080, "TCP"));

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForServiceWithNoPorts() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.0.0.100");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForNonExistentService() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace,
                        "use-cluster-ip", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService("svc");
        service.getServiceDiscovery().getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).isEmpty();
    }
}
