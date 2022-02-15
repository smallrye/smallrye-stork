package io.smallrye.stork.loadbalancer.poweroftwochoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class PowerOfTwoChoicesLoadBalancerTest {
    private static final Logger log = LoggerFactory.getLogger(PowerOfTwoChoicesLoadBalancerTest.class);

    public static final String FST_SRVC_1 = "localhost:8080";
    public static final String FST_SRVC_2 = "localhost:8081";
    public static final String FST_SRVC_3 = "localhost:8082";
    public static final String FST_SRVC_4 = "localhost:8083";
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", "power-of-two-choices", "static",
                null,
                Map.of("address-list", String.format("%s,%s,%s,%s", FST_SRVC_1, FST_SRVC_2, FST_SRVC_3, FST_SRVC_4)));
        TestConfigProvider.addServiceConfig("singleton-service", "power-of-two-choices",
                "static", null,
                Map.of("address-list", FST_SRVC_1));
        TestConfigProvider.addServiceConfig("without-instances", "power-of-two-choices",
                "empty-services", null, Collections.emptyMap());

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    public void shouldSelectLessLoadedAmongTwoWhenAllLoaded() {
        Service service = stork.getService("first-service");

        List<ServiceInstance> instances = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            instances.add(selectInstanceAndStart(service));
        }

        Random random = new Random();
        // Start reporting
        for (ServiceInstance instance : instances) {
            if (random.nextInt(10) > 7) {
                // Simulate failures
                mockRecordingTime(instance, 1000);
                instance.recordEnd(new Exception("boom"));
            } else {
                mockRecordingTime(instance, 1000);
            }

            ServiceInstance selected = selectInstance(service);
            assertThat(selected).isNotNull();
        }
    }

    @Test
    void shouldPickAllServicesEvenWhenNotReporting() {
        Service service = stork.getService("first-service");

        Set<String> instances = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            instances.add(asString(selectInstance(service)));
        }

        assertThat(instances).hasSize(4).contains(FST_SRVC_1, FST_SRVC_2, FST_SRVC_3, FST_SRVC_4);
    }

    @Test
    void shouldThrowNoServiceInstanceOnNoInstances() throws ExecutionException, InterruptedException {
        Service service = stork.getService("without-instances");

        CompletableFuture<Throwable> result = new CompletableFuture<>();

        service.selectServiceInstance().subscribe().with(v -> log.error("Unexpected successful result: {}", v),
                result::complete);

        await().atMost(Duration.ofSeconds(10)).until(result::isDone);
        assertThat(result.get()).isInstanceOf(NoServiceInstanceFoundException.class);
    }

    @Test
    void shouldReturnTheSingletonServiceWhenThereIsOnlyOne() {
        Service service = stork.getService("singleton-service");

        Set<String> set = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            set.add(asString(service.selectServiceInstance().await().atMost(Duration.ofMillis(5))));
        }

        assertThat(set).hasSize(1).contains(FST_SRVC_1);
    }

    @Test
    void selectServicesRandomlyWhenConcurrencyIs0() {
        Service service = stork.getService("first-service");

        Set<String> instances = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            ServiceInstance instance = selectInstance(service);
            mockRecordingTime(instance, 1);
            instances.add(asString(instance));
        }

        assertThat(instances).hasSize(4).contains(FST_SRVC_1, FST_SRVC_2, FST_SRVC_3, FST_SRVC_4);
    }

    private ServiceInstance selectInstance(Service service) {
        return service.selectServiceInstance().await().atMost(Duration.ofSeconds(5));
    }

    private String asString(ServiceInstance serviceInstance) {
        try {
            return String.format("%s:%s", serviceInstance.getHost(), serviceInstance.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ServiceInstance selectInstanceAndStart(Service service) {
        ServiceInstance serviceInstance = service.selectServiceInstance().await().atMost(Duration.ofSeconds(5));
        serviceInstance.recordStart(false);
        return serviceInstance;
    }

    @SuppressWarnings("deprecation")
    private void mockRecordingTime(ServiceInstance svc1, int timeInNs) {
        ((ServiceInstanceWithStatGathering) svc1).mockRecordingTime(timeInNs);
    }
}
