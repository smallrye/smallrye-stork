package io.smallrye.stork.loadbalancer.leastresponsetime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
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
        TestConfigProvider.addServiceConfig("first-service-secure-random", "least-response-time", "static",
                Map.of("use-secure-random", "true"),
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
        serviceInstance.recordStart(true);
        serviceInstance = selectInstance(service);
        assertThat(asString(serviceInstance)).isEqualTo(FST_SRVC_2);
    }

    @Test
    void testWithSecureRandom() {
        Service service = stork.getService("first-service-secure-random");

        ServiceInstance serviceInstance = selectInstance(service);
        assertThat(asString(serviceInstance)).isEqualTo(FST_SRVC_1);
        serviceInstance.recordStart(true);
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
    void shouldThrowNoServiceInstanceOnNoInstances() throws ExecutionException, InterruptedException {
        Service service = stork.getService("without-instances");

        CompletableFuture<Throwable> result = new CompletableFuture<>();

        service.selectServiceInstance().subscribe().with(v -> log.error("Unexpected successful result: {}", v),
                result::complete);

        await().atMost(Duration.ofSeconds(10)).until(result::isDone);
        assertThat(result.get()).isInstanceOf(NoServiceInstanceFoundException.class);
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
}
