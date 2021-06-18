package io.smallrye.dux.loadbalancer.roundrobin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.dux.Dux;
import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.test.TestConfigProvider;

public class RoundRobinLoadBalancerTest {

    public static final String FST_SRVC_1 = "http://localhost:8080";
    public static final String FST_SRVC_2 = "http://localhost:8081";
    private Dux dux;

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

        dux = new Dux();
    }

    @Test
    public void shouldGetServiceInstance() {
        LoadBalancer loadBalancer = dux.getLoadBalancer("first-service");

        assertThat(loadBalancer.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_1);
        assertThat(loadBalancer.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_2);
        assertThat(loadBalancer.selectServiceInstance().await().atMost(Duration.ofSeconds(5)).getValue()).isEqualTo(FST_SRVC_1);
    }
}
