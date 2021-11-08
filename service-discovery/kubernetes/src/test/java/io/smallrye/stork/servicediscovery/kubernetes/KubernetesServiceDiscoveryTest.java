package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.smallrye.stork.Metadata;
import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryTest {

    KubernetesClient client;

    String k8sMasterUrl;
    String k8sNamespace;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        k8sNamespace = client.getNamespace();
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", k8sNamespace));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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

    }

    @Test
    void shouldDiscoverServiceWithSpecificName() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", k8sNamespace, "application", "rest-service"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        setUpKubernetesService("rest-service", null, "10.96.96.231", "10.96.96.232", "10.96.96.233");
        setUpKubernetesService("svc", null, "10.95.95.125");

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
    }

    @Test
    void shouldGetServiceFromK8sNamespace() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "ns1"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesService(serviceName, "ns1", "10.96.96.231", "10.96.96.232", "10.96.96.233");

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
    }

    @Test
    void shouldGetServiceFromK8sAllNamespace() {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all"));
        Stork stork = StorkTestUtils.getNewStorkInstance();
        String serviceName = "svc";

        registerKubernetesService(serviceName, "ns1", "10.96.96.231", "10.96.96.232", "10.96.96.233");
        registerKubernetesService(serviceName, "ns2", "10.99.99.241", "10.99.99.242", "10.99.99.243");

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
    }

    @Test
    void shouldNotFetchWhenRefreshPeriodNotReached() {
        //Given a service `my-service` registered in k8s and a refresh-period of 5 minutes
        // 1- services instance are gathered form k8s
        // 2- we remove the service
        // when the k8s service discovery is called before the end of refreshing period
        // Then stork returns the instances from the cache
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", k8sNamespace));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", k8sNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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
    }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", k8sNamespace, "refresh-period", "3"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";

        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232", "10.96.96.233");

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
        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232");

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
        registerKubernetesService(serviceName, null, "10.96.96.231", "10.96.96.232", "10.96.96.234");

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

    private Map<String, Long> mapHostnameToIds(List<ServiceInstance> serviceInstances) {
        Map<String, Long> result = new HashMap<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.put(serviceInstance.getHost(), serviceInstance.getId());
        }
        return result;
    }

    private void registerKubernetesService(String serviceName, String namespace, String... ipAdresses) {

        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", "svc");
        labels.put("app.kubernetes.io/version", "1.0");
        List<EndpointAddress> endpointAddresses = Arrays.stream(ipAdresses)
                .map(ipAdress -> new EndpointAddressBuilder().withIp(ipAdress).build()).collect(Collectors.toList());
        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName(serviceName).withLabels(labels).endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddresses)
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace(namespace).withName(serviceName).create(endpoint);
    }

}
