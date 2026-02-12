package io.smallrye.stork.loadbalancer.leastresponsetime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class RandomLoadBalancerTest {
    private static final Logger log = Logger.getLogger(RandomLoadBalancerTest.class);

    public static final String FST_SRVC_1 = "localhost:8080";
    public static final String FST_SRVC_2 = "localhost:8081";
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", "random", "static", null,
                null, Map.of("address-list", String.format("%s,%s", FST_SRVC_1, FST_SRVC_2)), null);
        TestConfigProvider.addServiceConfig("first-service-secure-random", "random", "static", null,
                Map.of("use-secure-random", "true"), Map.of("address-list", String.format("%s,%s", FST_SRVC_1, FST_SRVC_2)),
                null);
        TestConfigProvider.addServiceConfig("singleton-service", "random", "static", null,
                null, Map.of("address-list", FST_SRVC_1), null);
        TestConfigProvider.addServiceConfig("without-instances", "random",
                "empty-services", null, null, Collections.emptyMap(), null);

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @ParameterizedTest
    @ValueSource(strings = { "first-service", "first-service-secure-random" })
    void shouldPickBothService(String serviceName) {
        Service service = stork.getService(serviceName);

        Set<String> instances = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            instances.add(asString(selectInstance(service)));
        }

        assertThat(instances).hasSize(2).contains(FST_SRVC_1, FST_SRVC_2);
    }

    @Test
    void shouldPickTheServiceWhenOnlyOne() {
        Service service = stork.getService("singleton-service");

        Set<String> instances = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            instances.add(asString(selectInstance(service)));
        }

        assertThat(instances).hasSize(1).contains(FST_SRVC_1);
    }

    @Test
    void shouldThrowNoServiceInstanceOnNoInstances() throws ExecutionException, InterruptedException {
        Service service = stork.getService("without-instances");

        CompletableFuture<Throwable> result = new CompletableFuture<>();

        service.selectInstance().subscribe().with(v -> log.errorf("Unexpected successful result: %s", v),
                result::complete);

        await().atMost(Duration.ofSeconds(10)).until(result::isDone);
        assertThat(result.get()).isInstanceOf(NoServiceInstanceFoundException.class);
    }

    private ServiceInstance selectInstance(Service service) {
        return service.selectInstance().await().atMost(Duration.ofSeconds(5));
    }

    private String asString(ServiceInstance serviceInstance) {
        try {
            return String.format("%s:%s", serviceInstance.getHost(), serviceInstance.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
