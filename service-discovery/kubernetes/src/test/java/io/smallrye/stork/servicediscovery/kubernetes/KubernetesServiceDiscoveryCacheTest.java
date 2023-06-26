package io.smallrye.stork.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.common.constraint.Assert;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceDiscoveryCacheTest {

    KubernetesMockServer server;

    KubernetesClient client;

    String k8sMasterUrl;
    String defaultNamespace;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        defaultNamespace = client.getNamespace();
    }

    //        @Test
    //        void shouldFetchInstancesFromTheCache() throws InterruptedException {
    //
    //            // Given a service with 3 instances registered in the cluster
    //            // Stork gather the cache from the cluster
    //            // Stork is called to get service instances again
    //            // Stork get the instances from the cache.
    //
    //            TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
    //                    null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace));
    //            Stork stork = StorkTestUtils.getNewStorkInstance();
    //
    //            String serviceName = "svc";
    //
    //            Endpoints endpoints = buildAndRegisterKubernetesService(serviceName, defaultNamespace, false, "10.96.96.231");
    //            Pod pod = buildAndRegisterBackendPod(serviceName, defaultNamespace, false, "10.96.96.231");
    //
    //            server.expect().get().withPath("/api/v1/namespaces/test/pods/svc-109696231")
    //                    .andReturn(HttpURLConnection.HTTP_OK, pod)
    //                    .once();
    //
    //            server.expect().get().withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3Dsvc")
    //                    .andReturn(HttpURLConnection.HTTP_OK, new KubernetesListBuilder().addToItems(endpoints).build()).once();
    //
    //            AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();
    //
    //            Service service = stork.getService(serviceName);
    //            service.getServiceDiscovery().getServiceInstances()
    //                    .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
    //                    .subscribe().with(instances::set);
    //
    //            await().atMost(Duration.ofSeconds(5))
    //                    .until(() -> instances.get() != null);
    //
    //            assertThat(instances.get()).hasSize(1);
    //            assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
    //            assertThat(instances.get().stream().map(ServiceInstance::getHost)).contains("10.96.96.231");
    //
    //            Thread.sleep(5000);
    //
    //            service.getServiceDiscovery().getServiceInstances()
    //                    .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
    //                    .subscribe().with(instances::set);
    //
    //            await().atMost(Duration.ofSeconds(5))
    //                    .until(() -> instances.get() != null);
    //
    //            assertThat(instances.get()).hasSize(1);
    //            assertThat(instances.get().stream().map(ServiceInstance::getPort)).allMatch(p -> p == 8080);
    //            assertThat(instances.get().stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder("10.96.96.231");
    //            for (ServiceInstance serviceInstance : instances.get()) {
    //                Map<String, String> labels = serviceInstance.getLabels();
    //                assertThat(labels).contains(entry("app.kubernetes.io/name", "svc"),
    //                        entry("app.kubernetes.io/version", "1.0"),
    //                        entry("ui", "ui-" + ipAsSuffix(serviceInstance.getHost())));
    //            }
    //
    //        }

    @Test
    void shouldPreserveIdsOnRefetch() throws InterruptedException {

        // Given a service with 3 instances registered in the cluster
        // Stork gathers the cache from the cluster
        // When endpoints are recreated with same IP and port
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances but it preserves the Stork service instances Id

        TestConfigProvider.addServiceConfig("svc", null, "kubernetes", null,
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace, "refresh-period", "3"), null);
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
        //
        //        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232");
        //
        //        //        Thread.sleep(5000);
        //
        //        service.getServiceDiscovery().getServiceInstances()
        //                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
        //                .subscribe().with(instances::set);
        //
        //        await().atMost(Duration.ofSeconds(5))
        //                .until(instances::get, Matchers.hasSize(2));
        //
        //        for (ServiceInstance serviceInstance : instances.get()) {
        //            assertThat(idsByHostname.get(serviceInstance.getHost())).isEqualTo(serviceInstance.getId());
        //        }
        //
        //        client.endpoints().withName(serviceName).delete();
        //        client.pods().withName("svc-109696231").delete();
        //        client.pods().withName("svc-109696232").delete();

        registerKubernetesResources(serviceName, defaultNamespace, "10.96.96.231", "10.96.96.232", "10.96.96.234");

        //        Thread.sleep(5000);

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
                        .addToPorts(new EndpointPortBuilder().withPort(8080).withProtocol("TCP").build())
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
