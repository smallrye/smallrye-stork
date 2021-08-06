package io.smallrye.stork.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryTest {

    KubernetesClient client;

    String k8sMasterUrl;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder()
                        .addToAddresses(new EndpointAddressBuilder().withIp("10.96.96.231").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.232").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.233").build())
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace("default").withName("svc").create(endpoint);

        List<Endpoints> items = client.endpoints().list().getItems();

        String serviceName = "svc";

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
    void shouldGetServiceFromK8sTestNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", client.getNamespace()));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder()
                        .addToAddresses(new EndpointAddressBuilder().withIp("10.96.96.231").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.232").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.233").build())
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace(client.getNamespace()).withName("svc").create(endpoint);

        List<Endpoints> items = client.endpoints().list().getItems();

        String serviceName = "svc";

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
    void shouldGetServiceFromK8sAllNamespace() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", "all"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder()
                        .addToAddresses(new EndpointAddressBuilder().withIp("10.96.96.231").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.232").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.233").build())
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        Endpoints endpoint2 = new EndpointsBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder()
                        .addToAddresses(new EndpointAddressBuilder().withIp("10.99.99.241").build(),
                                new EndpointAddressBuilder().withIp("10.99.99.242").build(),
                                new EndpointAddressBuilder().withIp("10.99.99.243").build())
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace("ns1").withName("svc").create(endpoint);
        client.endpoints().inNamespace("ns2").withName("svc").create(endpoint2);

        List<Endpoints> items = client.endpoints().list().getItems();

        String serviceName = "svc";

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
    }

    @Test
    void notBlockingShouldGetServiceFromK8s() throws InterruptedException {

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName("svc").endMetadata()
                .addToSubsets(new EndpointSubsetBuilder()
                        .addToAddresses(new EndpointAddressBuilder().withIp("10.96.96.231").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.232").build(),
                                new EndpointAddressBuilder().withIp("10.96.96.233").build())
                        .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                        .build())
                .build();

        client.endpoints().inNamespace("default").withName("svc").create(endpoint);

        List<Endpoints> items = client.endpoints().list().getItems();

        String serviceName = "svc";

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(10))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
        assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
        assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231",
                "10.96.96.232", "10.96.96.233");
    }

}
