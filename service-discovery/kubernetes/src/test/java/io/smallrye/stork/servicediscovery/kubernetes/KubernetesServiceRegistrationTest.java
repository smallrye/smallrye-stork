package io.smallrye.stork.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@EnableKubernetesMockClient(crud = true)
public class KubernetesServiceRegistrationTest {

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

    @Test
//    @Disabled("doesn't work yet")
    // TODO: fails with conflict on the second IP!
    void shouldRegisterServiceInstancesInDefaultNamespace() throws InterruptedException {
        TestConfigProvider.addServiceConfig("svc", null, "kubernetes",
                null, Map.of("k8s-host", k8sMasterUrl, "k8s-namespace", defaultNamespace));
        TestConfigProvider.addServiceRegistrarConfig("my-kube-registrar", "kubernetes", Map.of("k8s-host", k8sMasterUrl));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "svc";
        String[] ips = { "10.96.96.231", "10.96.96.232", "10.96.96.233" };

        ServiceRegistrar<KubernetesMetadataKey> kubeRegistrar = stork.getServiceRegistrar("my-kube-registrar");

        CountDownLatch registrationLatch = new CountDownLatch(ips.length);
        for (String ip : ips) {
            kubeRegistrar.registerServiceInstance(serviceName, Metadata.of(KubernetesMetadataKey.class)
                    .with(KubernetesMetadataKey.META_K8S_NAMESPACE, defaultNamespace), ip).subscribe()
                    .with(success -> registrationLatch.countDown(), failure -> fail(""));
        }
        if (!registrationLatch.await(10, TimeUnit.SECONDS)) {
            fail("Failed to register kubernetes services on time. Check the log above for details");
        }

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(3);
    }
}
