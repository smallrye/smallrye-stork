package io.smallrye.stork.loadbalancer.roundrobin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Service;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class RoundRobinLoadBalancerTest {

    public static final String FST_SRVC_1 = "http://localhost:8080";
    public static final String FST_SRVC_2 = "http://localhost:8081";
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", "round-robin", "static",
                null,
                Map.of("1", FST_SRVC_1, "2", FST_SRVC_2));

        TestConfigProvider.addServiceConfig("second-service", "round-robin", "static",
                null,
                Map.of("3", "http://localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "static",
                null,
                Map.of("4", "http://localhost:8083"));

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    public void shouldGetServiceInstance() {
        Service service = stork.getService("first-service");

        assertThat(service.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_1);
        assertThat(service.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_2);
        assertThat(service.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_1);
    }
}
