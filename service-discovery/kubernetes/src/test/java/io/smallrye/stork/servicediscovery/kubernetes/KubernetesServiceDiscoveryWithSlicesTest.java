package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.GroupVersionForDiscoveryBuilder;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointBuilder;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointConditionsBuilder;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSlice;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSliceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

/****
 * What these tests cover
 * Firm user override on both sides.
 * Successful auto-detection.
 * Endpointslices cluster auto-detection with fallback.
 *
 */
@DisabledOnOs(OS.WINDOWS)
@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryWithSlicesTest {

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
    void shouldUseEndpointSlicesWhenUserForcesIt() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", "test",
                        "use-endpoint-slices", "true"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231" };
        EndpointPort[] ports = { new EndpointPort("http", 8080) };

        registerKubernetesEndpointSlice(serviceName, "test", ips, ports);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes (EndpointSlices)", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactly(ips[0]);

        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(
                    entry("kubernetes.io/version", "1.0"),
                    entry("kubernetes.io/service-name", "svc"));
        }

        instances.get().stream()
                .map(ServiceInstance::getMetadata)
                .forEach(metadata -> {
                    Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
                    assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
                });

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldAggregateEndpointSlices() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", "test",
                        "use-endpoint-slices", "true"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips1 = { "10.96.96.231" };
        EndpointPort[] ports1 = { new EndpointPort("http", 8080) };

        String[] ips2 = { "10.96.96.232" };
        EndpointPort[] ports2 = { new EndpointPort("metrics", 9090) };

        registerKubernetesEndpointSlice(serviceName, "test", ips1, ports1);
        registerKubernetesEndpointSlice(serviceName, "test", ips2, ports2);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes (EndpointSlices)", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder(ips1[0], ips2[0]);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080, 9090);

        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(
                    entry("kubernetes.io/version", "1.0"),
                    entry("kubernetes.io/service-name", "svc"));
        }

        instances.get().stream()
                .map(ServiceInstance::getMetadata)
                .forEach(metadata -> {
                    Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
                    assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
                });

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldAggregateEndpointSlicesWithMultipleAddressesForSamePort() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", "test",
                        "use-endpoint-slices", "true"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips1 = { "10.96.96.231", "10.96.96.232" };
        EndpointPort[] ports1 = { new EndpointPort("http", 8080) };

        registerKubernetesEndpointSlice(serviceName, "test", ips1, ports1);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes (EndpointSlices)", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder(ips1[0], ips1[1]);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080, 8080);

        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(
                    entry("kubernetes.io/version", "1.0"),
                    entry("kubernetes.io/service-name", "svc"));
        }

        instances.get().stream()
                .map(ServiceInstance::getMetadata)
                .forEach(metadata -> {
                    Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
                    assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
                });

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldAggregateEndpointSlicesWithMultiplePorts() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", "test",
                        "use-endpoint-slices", "true"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips1 = { "10.96.96.231", "10.96.96.232" };
        EndpointPort[] ports1 = { new EndpointPort("http", 8080), new EndpointPort("management", 8081) };

        String[] ips2 = { "10.96.96.232" };
        EndpointPort[] ports2 = { new EndpointPort("metrics", 9090) };

        registerKubernetesEndpointSlice(serviceName, "test", ips1, ports1);
        registerKubernetesEndpointSlice(serviceName, "test", ips2, ports2);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes (EndpointSlices)", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(5);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder(ips1[0], ips1[1], ips1[0],
                ips1[1], ips2[0]);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).containsExactlyInAnyOrder(8080, 8080, 8081, 8081,
                9090);

        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(
                    entry("kubernetes.io/version", "1.0"),
                    entry("kubernetes.io/service-name", "svc"));
        }

        instances.get().stream()
                .map(ServiceInstance::getMetadata)
                .forEach(metadata -> {
                    Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
                    assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
                });

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isFalse());
    }

    @Test
    void shouldUseEndpointsWhenUserDisablesEndpointSlices() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", defaultNamespace,
                        "use-endpoint-slices", "false"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        whenGetSlicesApiAvailableThenReturnTrue();

        String serviceNameSlice = "svc";
        String[] ipsSlice = { "10.0.0.99" }; // <- this ip should be ignored, because the user disabled EndpointSlices
        EndpointPort[] portsSlice = { new EndpointPort("http", 8080) };
        registerKubernetesEndpointSlice(serviceNameSlice, "test", ipsSlice, portsSlice);

        String serviceName = "svc";
        String[] endpointIps = { "10.0.0.2" };
        String[] sliceIps = { "10.0.0.99" };
        EndpointPort[] slicePorts = { new EndpointPort("http", 9090) };

        utils.registerKubernetesLegacyEndpointsResources(serviceName, defaultNamespace, endpointIps);
        registerKubernetesEndpointSlice(serviceName, defaultNamespace, sliceIps, slicePorts);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        stork.getService(serviceName)
                .getServiceDiscovery()
                .getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("10.0.0.2");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8080);
    }

    @Test
    void shouldAutoDetectAndUseEndpointSlicesWhenAvailable() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", defaultNamespace),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.0.0.3" };
        EndpointPort[] ports = { new EndpointPort("http", 8080) };

        whenGetSlicesApiAvailableThenReturnTrue();

        registerKubernetesEndpointSlice(serviceName, defaultNamespace, ips, ports);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        stork.getService(serviceName)
                .getServiceDiscovery()
                .getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("10.0.0.3");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8080);
    }

    @Test
    void shouldGetServiceUsingSelectedPortNameWhenMultiplePortsFromSpecificNamespace() {
        String serviceName = "svc";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl,
                        "k8s-namespace", "ns1",
                        "port-name", "http",
                        "use-endpoint-slices", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        whenGetSlicesApiAvailableThenReturnTrue();

        String[] ips1 = { "10.96.96.231", "10.96.96.232" };
        EndpointPort[] ports1 = { new EndpointPort("http", 8080), new EndpointPort("management", 8081) };

        registerKubernetesEndpointSlice(serviceName, "ns1", ips1, ports1);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232");
        for (ServiceInstance serviceInstance : instances.get()) {
            Map<String, String> labels = serviceInstance.getLabels();
            assertThat(labels).contains(
                    entry("kubernetes.io/service-name", "svc"),
                    entry("kubernetes.io/version", "1.0"));
        }
        instances.get().stream().map(ServiceInstance::getMetadata).forEach(metadata -> {
            Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) metadata;
            assertThat(k8sMetadata.getMetadata()).containsKey(META_K8S_SERVICE_ID);
        });
    }

    @Test
    void shouldReturnNoInstancesWhenPortNameMatchesNothing() {
        String serviceName = "svc";

        TestConfigProvider.addServiceConfig(serviceName, null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl,
                        "k8s-namespace", "ns1",
                        "port-name", "metrics",
                        "use-endpoint-slices", "true"),
                null);
        Stork stork = StorkTestUtils.getNewStorkInstance();

        whenGetSlicesApiAvailableThenReturnTrue();

        String[] ips = { "10.96.96.231", "10.96.96.232" };
        EndpointPort[] ports = { new EndpointPort("http", 8080) };

        registerKubernetesEndpointSlice(serviceName, "ns1", ips, ports);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(0);
    }

    private void whenGetSlicesApiAvailableThenReturnTrue() {
        server.expect()
                .get()
                .withPath("/apis")
                .andReply(200, r -> {
                    APIGroupList build = new APIGroupListBuilder().addNewGroup()
                            .withName("discovery.k8s.io")
                            .withVersions(new GroupVersionForDiscoveryBuilder()
                                    .withGroupVersion("discovery.k8s.io/v1")
                                    .withVersion("v1")
                                    .build())
                            .withPreferredVersion(new GroupVersionForDiscoveryBuilder()
                                    .withGroupVersion("discovery.k8s.io/v1")
                                    .withVersion("v1")
                                    .build())
                            .endGroup().build();

                    return build;
                })
                .always();
    }

    @Test
    void shouldFallbackToEndpointsWhenNoEndpointSlicesExist() {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null,
                Map.of(
                        "k8s-host", k8sMasterUrl,
                        "k8s-namespace", defaultNamespace),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.0.0.4" };

        utils.registerKubernetesLegacyEndpointsResources(serviceName, defaultNamespace, ips); // <-- Endpoints

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        stork.getService(serviceName)
                .getServiceDiscovery()
                .getServiceInstances()
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("10.0.0.4");
    }

    /**
     * Registers one Kubernetes EndpointSlice for the given service.
     *
     * <p>
     * Each EndpointSlice represents a homogeneous group of endpoints, meaning
     * that all addresses in the slice share the same set of ports. Different port
     * combinations are therefore modeled as separate EndpointSlices.
     * </p>
     *
     * @param serviceName the Kubernetes Service name
     * @param namespace the Kubernetes namespace
     * @param ips the IP addresses exposed by the service
     * @param ports the ports exposed by those IPs
     */
    private void registerKubernetesEndpointSlice(String serviceName, String namespace, String[] ips, EndpointPort[] ports) {
        List<io.fabric8.kubernetes.api.model.discovery.v1.EndpointPort> portList = new ArrayList<>();
        for (EndpointPort port : ports) {
            portList.add(new io.fabric8.kubernetes.api.model.discovery.v1.EndpointPortBuilder().withPort(port.portNumber())
                    .withName(port.name()).build());
        }
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
        EndpointSlice slice = new EndpointSliceBuilder()
                .withNewMetadata()
                .withName(serviceName + "- endpointSlice-" + randomSuffix)
                .withNamespace(namespace)
                .withLabels(Map.of(
                        "kubernetes.io/service-name", serviceName,
                        "kubernetes.io/version", "1.0"))
                .endMetadata()
                .withAddressType("IPv4")
                .withPorts(portList)
                .withEndpoints(new EndpointBuilder()
                        .withAddresses(Arrays.asList(ips))
                        .withConditions(new EndpointConditionsBuilder().withReady(true).build())
                        .build())
                .build();

        client.discovery().v1().endpointSlices()
                .inNamespace(namespace)
                .resource(slice)
                .create();

    }

    record EndpointPort(String name, int portNumber) {
    }

}
