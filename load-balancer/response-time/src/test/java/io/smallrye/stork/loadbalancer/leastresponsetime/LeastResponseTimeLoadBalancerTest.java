package io.smallrye.stork.loadbalancer.leastresponsetime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class LeastResponseTimeLoadBalancerTest {

    public static final String FST_SRVC_1 = "localhost:8080";
    public static final String FST_SRVC_2 = "localhost:8081";
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", "least-response-time", "static",
                null,
                Map.of("1", FST_SRVC_1, "2", FST_SRVC_2));

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldSelectNotSelectedFirst() {
        Service service = stork.getService("first-service");

        assertThat(asString(selectInstance(service))).isEqualTo(FST_SRVC_1);
        assertThat(asString(selectInstance(service))).isEqualTo(FST_SRVC_2);
    }

    @Test
    void shouldSelectNonFailing() {
        Service service = stork.getService("first-service");

        // svc1 is not that fast
        ServiceInstance svc1 = selectInstance(service);
        assertThat(asString(svc1)).isEqualTo(FST_SRVC_1);
        svc1.recordResult(80, null);

        // svc2 is faster
        ServiceInstance svc2 = selectInstance(service);
        assertThat(asString(svc2)).isEqualTo(FST_SRVC_2);
        svc2.recordResult(50, null);

        ServiceInstance selected;

        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        // but svc2 sometimes fails
        svc2.recordResult(10, new RuntimeException("induced failure"));

        // so we should select svc1 for next calls
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);
    }

    @Test
    void shouldSelectFastest() {
        Service service = stork.getService("first-service");

        ServiceInstance svc1 = selectInstance(service);
        assertThat(asString(svc1)).isEqualTo(FST_SRVC_1);
        svc1.recordResult(100, null);

        ServiceInstance svc2 = selectInstance(service);
        assertThat(asString(svc2)).isEqualTo(FST_SRVC_2);
        svc2.recordResult(10, null);

        ServiceInstance selected;

        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        svc2.recordResult(10, null);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        svc2.recordResult(10, null);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

        svc2.recordResult(1000, null);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_1);

        svc1.recordResult(1000, null);
        selected = selectInstance(service);
        assertThat(asString(selected)).isEqualTo(FST_SRVC_2);

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
