package io.smallrye.stork.loadbalancer.leastresponsetime;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;
import io.smallrye.stork.loadbalancer.leastresponsetime.impl.TestUtils;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class LeastResponseTimeLoadBalancerTest {
    private static final Logger log = LoggerFactory.getLogger(LeastResponseTimeLoadBalancerTest.class);

    public static final String FST_SRVC_1 = "localhost:8080";
    public static final String FST_SRVC_2 = "localhost:8081";
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", "least-response-time", "static",
                null,
                Map.of("address-list", String.format("%s,%s", FST_SRVC_1, FST_SRVC_2)));
        TestConfigProvider.addServiceConfig("without-instances", "least-response-time",
                "empty-services", null, Collections.emptyMap());

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldSelectNotSelectedFirst() {
        Service service = stork.getService("first-service");

        ServiceInstance serviceInstance = selectInstance(service);
        assertThat(asString(serviceInstance)).isEqualTo(FST_SRVC_1);
        serviceInstance = selectInstance(service);
        assertThat(asString(serviceInstance)).isEqualTo(FST_SRVC_2);
    }

    @Test
    void shouldSelectNonFailing() {
        Service service = stork.getService("first-service");

        // svc1 is not that fast
        ServiceInstance svc1 = selectInstance(service);
        assertThat(asString(svc1)).isEqualTo(FST_SRVC_1);
        int timeInNs = 80;
        mockRecordingTime(svc1, timeInNs);

        // svc2 is faster
        ServiceInstance svc2 = selectInstance(service);
        assertThat(asString(svc2)).isEqualTo(FST_SRVC_2);
        mockRecordingTime(svc2, 50);

        ServiceInstance selected;

        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        // but svc2 sometimes fails

        mockRecordingTime(svc2, 10);
        svc2.recordEnd(new RuntimeException("induced failure"));

        // so we should select svc1 for next calls
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);
    }

    @SuppressWarnings("deprecation")
    private void mockRecordingTime(ServiceInstance svc1, int timeInNs) {
        ((ServiceInstanceWithStatGathering) svc1).mockRecordingTime(timeInNs);
    }

    @Test
    void shouldSelectFastest() {
        Service service = stork.getService("first-service");

        ServiceInstance svc1 = selectInstance(service);
        assertThat(asString(svc1)).isEqualTo(FST_SRVC_1);
        mockRecordingTime(svc1, 100);

        ServiceInstance svc2 = selectInstance(service);
        assertThat(asString(svc2)).isEqualTo(FST_SRVC_2);
        mockRecordingTime(svc2, 10);

        ServiceInstance selected;

        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        mockRecordingTime(svc2, 10);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        mockRecordingTime(svc2, 10);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        mockRecordingTime(svc2, 1000);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);

        mockRecordingTime(svc1, 1000);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);
    }

    @Test
    void shouldThrowNoServiceInstanceOnNoInstances() throws ExecutionException, InterruptedException, TimeoutException {
        Service service = stork.getService("first-service");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<ServiceInstance>> callables = asList(
                () -> service.selectInstanceAndRecordStart(true).await().atMost(Duration.ofSeconds(5)),
                () -> service.selectInstanceAndRecordStart(true).await().atMost(Duration.ofSeconds(5)));
        for (int i = 0; i < 20; i++) { // let's test it a few times
            List<Future<ServiceInstance>> futures = executor.invokeAll(callables);

            Set<Long> serviceIds = new HashSet<>();
            for (Future<ServiceInstance> future : futures) {
                ServiceInstance serviceInstance = future.get(5, TimeUnit.SECONDS);
                serviceIds.add(serviceInstance.getId());
            }
            assertThat(serviceIds).hasSize(2); // just make sure different instances are selected

            clearStats((LeastResponseTimeLoadBalancer) service.getLoadBalancer());
        }
    }

    @Test
    void shouldSelectAllAvailableWhenInvokedInParallel() throws ExecutionException, InterruptedException {
        Service service = stork.getService("without-instances");

        CompletableFuture<Throwable> result = new CompletableFuture<>();

        service.selectInstance().subscribe().with(v -> log.error("Unexpected successful result: {}", v),
                result::complete);

        await().atMost(Duration.ofSeconds(10)).until(result::isDone);
        assertThat(result.get()).isInstanceOf(NoServiceInstanceFoundException.class);
    }

    @SuppressWarnings("deprecation")
    private void clearStats(LeastResponseTimeLoadBalancer balancer) {
        TestUtils.clear(balancer.getCallStatistics());
    }

    private ServiceInstance selectInstance(Service service) {
        return service.selectInstanceAndRecordStart(true).await().atMost(Duration.ofSeconds(5));
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
