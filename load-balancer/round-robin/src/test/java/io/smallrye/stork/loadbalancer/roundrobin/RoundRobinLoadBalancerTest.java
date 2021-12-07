package io.smallrye.stork.loadbalancer.roundrobin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.NoServiceInstanceFoundException;
import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;

public class RoundRobinLoadBalancerTest {

    private static final Logger log = Logger.getLogger(RoundRobinLoadBalancerTest.class);

    public static final String FST_SRVC_1 = "localhost:8080";
    public static final String FST_SRVC_1_HTTP = String.format("http://%s", FST_SRVC_1);
    public static final String FST_SRVC_2 = "localhost:8081";
    public static final String FST_SRVC_2_HTTP = String.format("http://%s", FST_SRVC_2);
    private Stork stork;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("without-instances", "round-robin", "static",
                null,
                Collections.emptyMap());
        TestConfigProvider.addServiceConfig("first-service", "round-robin", "static",
                null,
                Map.of("1", FST_SRVC_1, "2", FST_SRVC_2));

        TestConfigProvider.addServiceConfig("second-service", "round-robin", "static",
                null,
                Map.of("3", "localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "static",
                null,
                Map.of("4", "localhost:8083"));

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    public void shouldGetServiceInstance() {
        Service service = stork.getService("first-service");

        assertThat(selectInstance(service)).isEqualTo(FST_SRVC_1_HTTP);
        assertThat(selectInstance(service)).isEqualTo(FST_SRVC_2_HTTP);
        assertThat(selectInstance(service)).isEqualTo(FST_SRVC_1_HTTP);
    }

    @Test
    void shouldThrowNoServiceInstanceOnNoInstances() throws ExecutionException, InterruptedException {
        Service service = stork.getService("without-instances");

        CompletableFuture<Throwable> result = new CompletableFuture<>();

        service.selectServiceInstance().subscribe().with(v -> log.errorf("Unexpected successful result: %s", v),
                result::complete);

        await().atMost(Duration.ofSeconds(10)).until(result::isDone);
        assertThat(result.get()).isInstanceOf(NoServiceInstanceFoundException.class);
    }

    private String selectInstance(Service service) {
        try {
            ServiceInstance serviceInstance = service.selectServiceInstance().await().atMost(Duration.ofSeconds(5));
            return String.format("http://%s:%s", serviceInstance.getHost(), serviceInstance.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
