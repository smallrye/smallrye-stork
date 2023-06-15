package io.smallrye.stork.servicediscovery.knative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

@Disabled
public class KnativeServiceDiscoveryRealClusterTest {

    String k8sMasterUrl;
    String namespace;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
    }

    @Test
    void shouldDiscoverHeroesKnative() {
        String svc = "hero-service";

        TestConfigProvider.addServiceConfig(svc, null, "knative", null,
                null, Map.of("knative-host", "https://api.sandbox-m2.ll9k.p1.openshiftapps.com:6443", "knative-namespace",
                        "amunozhe-dev", "application", "rest-heroes"),
                null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(svc);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(40))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance svcInstance = instances.get().get(0);

        // This code needs the smallrye-mutiny-vertx-web-client dependency
        //        WebClient client = WebClient.create(Vertx.vertx(), new WebClientOptions()
        //                .setDefaultHost(svcInstance.getHost()).setDefaultPort(svcInstance.getPort()).setSsl(false).setTrustAll(false));
        //
        //        await().untilAsserted(() -> Assertions.assertEquals(200,
        //                client.get("/api/heroes/random").sendAndAwait().statusCode()));

    }

    //    @Test
    void shouldDiscoverNamespacedKnativeServicesWithApp() {
        String svc = "my-service";

        TestConfigProvider.addServiceConfig("my-service", null, "knative", null,
                null, Map.of("knative-namespace", "default", "application", "helloworld-go"), null);

        Stork stork = StorkTestUtils.getNewStorkInstance();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        io.smallrye.stork.api.Service service = stork.getService(svc);
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from the cluster", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
        ServiceInstance svcInstance = instances.get().get(0);

        //        WebClient client = WebClient.create(Vertx.vertx(), new WebClientOptions()
        //                .setDefaultHost(svcInstance.getHost()).setSsl(false).setTrustAll(false));
        //
        //        await().untilAsserted(() -> Assertions.assertEquals(200,
        //                client.get("").sendAndAwait().statusCode()));

    }

}
