package io.smallrye.stork.servicediscovery.kubernetes;

import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesTestUtils.servicePort;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.Vertx;

/**
 * Verifies that the Kubernetes service discovery informer setup does not block
 * the Vert.x event loop.
 *
 * @see <a href="https://github.com/smallrye/smallrye-stork/issues/1274">smallrye-stork#1274</a>
 */
@DisabledOnOs(OS.WINDOWS)
@EnableKubernetesMockClient(crud = true)
class KubernetesServiceDiscoveryEventLoopTest {

    KubernetesClient client;
    KubernetesMockServer server;
    String k8sMasterUrl;
    String defaultNamespace;
    KubernetesTestUtils utils;

    @BeforeEach
    void setUp() {
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        k8sMasterUrl = client.getMasterUrl().toString();
        defaultNamespace = client.getNamespace();
        utils = new KubernetesTestUtils(client);
    }

    @Test
    void informerSetupShouldNotBlockEventLoop() throws Exception {
        // Override the endpoints LIST path with a delayed response to simulate
        // real Kubernetes API latency during informer initialization.
        int delayMs = 500;
        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace + "/endpoints?resourceVersion=0")
                .andReply(200, r -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new EndpointsListBuilder().build();
                })
                .always();

        // Also handle the watch request so the informer doesn't fail
        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace
                        + "/endpoints?allowWatchBookmarks=true&resourceVersion=0&timeoutSeconds=600&watch=true")
                .andReply(200, r -> new EndpointsBuilder().build())
                .always();

        KubernetesConfiguration config = new KubernetesConfiguration(Map.of(
                "k8s-host", k8sMasterUrl,
                "k8s-namespace", defaultNamespace,
                "use-endpoint-slices", "false"));

        Vertx vertx = Vertx.vertx();
        try {
            KubernetesServiceDiscovery discovery = new KubernetesServiceDiscovery("svc", config, vertx);

            CountDownLatch timerLatch = new CountDownLatch(1);
            CountDownLatch resultLatch = new CountDownLatch(1);
            AtomicLong timerFiredAt = new AtomicLong();
            AtomicReference<Throwable> error = new AtomicReference<>();

            long start = System.nanoTime();

            // Subscribe from the event loop to verify that the informer setup does not block it.
            vertx.runOnContext(v -> {
                discovery.getServiceInstances().subscribe().with(
                        instances -> {
                            resultLatch.countDown();
                        },
                        err -> {
                            error.set(err);
                            resultLatch.countDown();
                        });

                // Timer on the same event loop: fires quickly only if the event loop is free
                vertx.setTimer(50, id -> {
                    timerFiredAt.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                    timerLatch.countDown();
                });
            });
            assertThat(timerLatch.await(10, TimeUnit.SECONDS))
                    .as("Timer should have fired")
                    .isTrue();
            assertThat(resultLatch.await(10, TimeUnit.SECONDS))
                    .as("Service discovery should complete")
                    .isTrue();

            // If the event loop was blocked by the 500ms informer setup,
            // the timer would fire after ~500ms instead of ~50ms
            assertThat(timerFiredAt.get())
                    .as("Event loop should not be blocked during informer setup")
                    .isLessThan(300);

            assertThat(error.get()).isNull();
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void failedServicesInformerSetupShouldBeRetried() throws Exception {
        AtomicInteger servicesListCalls = new AtomicInteger();

        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace + "/services?resourceVersion=0")
                .andReply(500, r -> {
                    servicesListCalls.incrementAndGet();
                    return "";
                })
                .once();

        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace + "/services?resourceVersion=0")
                .andReply(200, r -> {
                    servicesListCalls.incrementAndGet();
                    return new ServiceListBuilder().build();
                })
                .once();

        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.96.0.1",
                servicePort("http", 8080, "TCP"));

        KubernetesConfiguration config = new KubernetesConfiguration(Map.of(
                "k8s-host", k8sMasterUrl,
                "k8s-namespace", defaultNamespace,
                "use-cluster-ip", "true"));

        Vertx vertx = Vertx.vertx();
        try {
            KubernetesServiceDiscovery discovery = new KubernetesServiceDiscovery("svc", config, vertx);

            List<ServiceInstance> first = discovery.getServiceInstances().await().atMost(Duration.ofSeconds(5));
            List<ServiceInstance> second = discovery.getServiceInstances().await().atMost(Duration.ofSeconds(5));

            assertThat(first).hasSize(1);
            assertThat(second).hasSize(1);
            assertThat(second.get(0).getHost()).isEqualTo("10.96.0.1");
            assertThat(second.get(0).getPort()).isEqualTo(8080);
            assertThat(servicesListCalls.get()).isEqualTo(2);
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void servicesInformerSetupShouldNotBlockEventLoop() throws Exception {
        int delayMs = 500;

        // Delay the services informer LIST path
        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace + "/services?resourceVersion=0")
                .andReply(200, r -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new ServiceListBuilder().build();
                })
                .always();

        // Handle watch request with the query params sent by fabric8 informer
        server.expect().get()
                .withPath("/api/v1/namespaces/" + defaultNamespace
                        + "/services?allowWatchBookmarks=true&resourceVersion=1&timeoutSeconds=600&watch=true")
                .andReply(200, r -> new ServiceListBuilder().build())
                .always();

        // Register a ClusterIP service for the fetch to return
        utils.registerKubernetesServiceResource("svc", defaultNamespace, "10.96.0.1",
                servicePort("http", 8080, "TCP"));

        KubernetesConfiguration config = new KubernetesConfiguration(Map.of(
                "k8s-host", k8sMasterUrl,
                "k8s-namespace", defaultNamespace,
                "use-cluster-ip", "true"));

        Vertx vertx = Vertx.vertx();
        try {
            CountDownLatch timerLatch = new CountDownLatch(1);
            CountDownLatch resultLatch = new CountDownLatch(1);
            AtomicLong timerFiredAt = new AtomicLong();
            AtomicReference<List<ServiceInstance>> result = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            long start = System.nanoTime();

            // Create the discovery AND subscribe on the event loop.
            // Before the fix, the constructor called configureServicesInformer()
            // synchronously, blocking the event loop during construction.
            vertx.runOnContext(v -> {
                KubernetesServiceDiscovery discovery = new KubernetesServiceDiscovery("svc", config, vertx);

                discovery.getServiceInstances().subscribe().with(
                        instances -> {
                            result.set(instances);
                            resultLatch.countDown();
                        },
                        err -> {
                            error.set(err);
                            resultLatch.countDown();
                        });

                vertx.setTimer(50, id -> {
                    timerFiredAt.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                    timerLatch.countDown();
                });
            });

            assertThat(timerLatch.await(10, TimeUnit.SECONDS))
                    .as("Timer should have fired")
                    .isTrue();
            assertThat(resultLatch.await(10, TimeUnit.SECONDS))
                    .as("Service discovery should complete")
                    .isTrue();

            assertThat(timerFiredAt.get())
                    .as("Event loop should not be blocked during services informer setup")
                    .isLessThan(300);

            assertThat(error.get()).isNull();
            assertThat(result.get()).hasSize(1);
            assertThat(result.get().get(0).getHost()).isEqualTo("10.96.0.1");
            assertThat(result.get().get(0).getPort()).isEqualTo(8080);
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }
}
