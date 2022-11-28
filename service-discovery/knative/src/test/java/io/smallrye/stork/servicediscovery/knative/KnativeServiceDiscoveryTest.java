package io.smallrye.stork.servicediscovery.knative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.knative.serving.v1.ServiceStatusBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KnativeServiceDiscoveryTest {

    KnativeClient kn;

    KubernetesMockServer server;

    String k8sMasterUrl;
    String namespace;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = kn.getMasterUrl().toString();
        namespace = kn.getNamespace();
    }

    @Test
    void shouldDiscoverKnativeService() {
        TestConfigProvider.addServiceConfig("ksvc", null, "knative",
                null, Map.of("knative-host", k8sMasterUrl, "knative-namespace", "test"));

        Stork stork = StorkTestUtils.getNewStorkInstance();

        String knativeService = "ksvc";

        ServiceStatus serviceStatus = new ServiceStatusBuilder().withUrl("http://hello.kndefault.127.0.0.1.sslip.io").build();
        io.fabric8.knative.serving.v1.Service knSvc = new ServiceBuilder().withNewMetadata().withName(knativeService)
                .endMetadata().withStatus(serviceStatus)
                .build();
        kn.services().inNamespace(namespace).resource(knSvc).create();

        List<Service> items = kn.services().inNamespace(namespace).list().getItems();

        assertThat(items).isNotEmpty();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(knativeService);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        assertThat(instances.get().get(0).getHost()).isEqualTo("http://hello.kndefault.127.0.0.1.sslip.io");
        assertThat(instances.get().get(0).getPort()).isEqualTo(8080);
    }
}
