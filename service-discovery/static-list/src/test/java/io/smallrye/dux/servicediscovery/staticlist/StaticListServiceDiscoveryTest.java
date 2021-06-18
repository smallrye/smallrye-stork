package io.smallrye.dux.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.dux.Dux;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.test.TestConfigProvider;

public class StaticListServiceDiscoveryTest {

    Dux dux;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        TestConfigProvider.addServiceConfig("first-service", null, "static",
                null, Map.of("1", "http://localhost:8080", "2", "http://localhost:8081"));

        TestConfigProvider.addServiceConfig("second-service", null, "static",
                null, Map.of("3", "http://localhost:8082"));

        TestConfigProvider.addServiceConfig("third-service", null, "static",
                null, Map.of("4", "http://localhost:8083"));

        dux = new Dux();
    }

    @Test
    void shouldGetAllServiceInstances() {
        List<ServiceInstance> serviceInstances = Dux.getInstance().getServiceDiscovery("first-service")
                .getServiceInstances()
                .collect().asList().await().atMost(Duration.ofSeconds(5));

        assertThat(serviceInstances).hasSize(2);
        assertThat(serviceInstances.stream().map(ServiceInstance::getValue)).containsExactlyInAnyOrder("http://localhost:8080",
                "http://localhost:8081");
    }

}
