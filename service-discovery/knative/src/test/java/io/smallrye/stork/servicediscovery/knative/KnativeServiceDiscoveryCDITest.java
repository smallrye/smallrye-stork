package io.smallrye.stork.servicediscovery.knative;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.knative.serving.v1.ServiceStatusBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;

@ExtendWith(WeldJunit5Extension.class)
@EnableKubernetesMockClient(crud = true)
public class KnativeServiceDiscoveryCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            KnativeServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    KnativeClient kn;

    KubernetesMockServer server;

    String k8sMasterUrl;
    String namespace;

    @BeforeEach
    void setUp() {
        config.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = kn.getMasterUrl().toString();
        namespace = kn.getNamespace();
    }

    @Test
    void shouldDiscoverNamespacedKnativeServices() {
        config.addServiceConfig("my-knservice", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "test"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String knSvcName = "my-knservice";

        registerKnativeServices(knSvcName, "http://hello.test.127.0.0.1.sslip.io", null);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knSvcName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("hello.test.127.0.0.1.sslip.io");
        assertThat(instances.get().get(0).getPort()).isEqualTo(0);
        Map<String, String> labels = instances.get().get(0).getLabels();
        assertThat(labels).contains(entry("serving.knative.dev/creator", "kubernetes-admin"),
                entry("serving.knative.dev/lastModifier", "kubernetes-admin"));
    }

    private void registerKnativeServices(String knativeService, String url, String namespace) {

        Service knSvc = buildKnService(knativeService, url, namespace);
        if (namespace != null) {
            kn.services().inNamespace(namespace).resource(knSvc).create();
        } else {
            kn.services().resource(knSvc).create();
        }
    }

    private static Service buildKnService(String knativeService, String url, String namespace) {
        Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put("serving.knative.dev/creator", "kubernetes-admin");
        serviceLabels.put("serving.knative.dev/lastModifier", "kubernetes-admin");

        ServiceStatus serviceStatus = new ServiceStatusBuilder().withUrl(url)
                .withLatestCreatedRevisionName("revisionName").build();
        Service knSvc = new ServiceBuilder().withNewMetadata().withNamespace(namespace).withLabels(serviceLabels)
                .withName(knativeService)
                .endMetadata().withStatus(serviceStatus)
                .build();
        return knSvc;
    }

    @Test
    void shouldDiscoverKnativeServicesInAllNs() {
        config.addServiceConfig("my-knservice", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "all"));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String knativeService = "my-knservice";

        registerKnativeServices(knativeService, "hello.ns1.127.0.0.1.sslip.io", "ns1");
        registerKnativeServices(knativeService, "hello.ns2.127.0.0.1.sslip.io", "ns2");

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knativeService);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(instances.get().stream().map(ServiceInstance::getHost))
                .containsExactlyInAnyOrder("hello.ns1.127.0.0.1.sslip.io", "hello.ns2.127.0.0.1.sslip.io");
    }

    @Test
    void shouldGetServiceFromK8sDefaultNamespaceUsingProgrammaticAPI() {
        Stork stork = StorkTestUtils.getNewStorkInstance();
        stork.defineIfAbsent("my-knservice", ServiceDefinition.of(
                new KnativeConfiguration().withKnativeHost(k8sMasterUrl).withKnativeNamespace(namespace)));

        String knativeService = "my-knservice";

        registerKnativeServices(knativeService, "hello.test.127.0.0.1.sslip.io", null);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knativeService);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("hello.test.127.0.0.1.sslip.io");
        assertThat(instances.get().get(0).getPort()).isEqualTo(0);
        Map<String, String> labels = instances.get().get(0).getLabels();
        assertThat(labels).contains(entry("serving.knative.dev/creator", "kubernetes-admin"),
                entry("serving.knative.dev/lastModifier", "kubernetes-admin"));
    }

    @Test
    void shouldHandleSecureAttribute() {

        config.addServiceConfig("my-knservice", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "test", "secure", "true"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String knSvcName = "my-knservice";

        registerKnativeServices(knSvcName, "hello.test.127.0.0.1.sslip.io", null);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knSvcName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("hello.test.127.0.0.1.sslip.io");
        assertThat(instances.get().get(0).getPort()).isEqualTo(0);
        Map<String, String> labels = instances.get().get(0).getLabels();
        assertThat(labels).contains(entry("serving.knative.dev/creator", "kubernetes-admin"),
                entry("serving.knative.dev/lastModifier", "kubernetes-admin"));

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isTrue());

    }

    @RepeatedTest(10)
    void shouldFetchInstancesFromTheClusterWhenCacheIsInvalidated() {

        // Given a service with 3 instances registered in the cluster
        // Stork gather the cache from the cluster
        // When the endpoints are removed (this invalidates the cache)
        // Stork is called to get service instances again
        // Stork contacts the cluster to get the instances : it gets 0 of them

        config.addServiceConfig("my-knservice", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "test", "secure", "true"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String knSvcName = "my-knservice";

        registerKnativeServices(knSvcName, "hello.test.127.0.0.1.sslip.io", null);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knSvcName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("hello.test.127.0.0.1.sslip.io");
        assertThat(instances.get().get(0).getPort()).isEqualTo(0);
        Map<String, String> labels = instances.get().get(0).getLabels();
        assertThat(labels).contains(entry("serving.knative.dev/creator", "kubernetes-admin"),
                entry("serving.knative.dev/lastModifier", "kubernetes-admin"));

        assertThat(instances.get()).allSatisfy(si -> assertThat(si.isSecure()).isTrue());

        kn.services().withName(knSvcName).delete();

        await().atMost(Duration.ofSeconds(5))
                .until(() -> {
                    // Need to redo the query until the cache is invalidated.
                    service.getServiceDiscovery().getServiceInstances()
                            .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                            .subscribe().with(instances::set);
                    return instances.get() != null && instances.get().size() == 0;
                });
    }

    @Test
    void shouldFetchInstancesFromTheCache() throws InterruptedException {

        // Stork gathers the cache from the cluster
        // Configure the mock cluster for recordings calls to a specific path
        // Stork is called twice to get service instances
        // Stork get the instances from the cache: assert that only 1 call to the cluster has been done

        String knSvcName = "my-knservice";

        //Recording k8s cluster calls and build a Knative Service as response
        AtomicInteger serverHit = new AtomicInteger(0);
        server.expect().get().withPath("/apis/serving.knative.dev/v1/namespaces/test/services/my-knservice")
                .andReply(200, r -> {
                    serverHit.incrementAndGet();
                    return buildKnService(knSvcName, "hello.test.127.0.0.1.sslip.io", "test");
                }).always();

        config.addServiceConfig("my-knservice", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "test", "secure", "true"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knSvcName);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);

        //second try to get instances, instances should be fetched from cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(serverHit.get()).isEqualTo(1);

    }

}
