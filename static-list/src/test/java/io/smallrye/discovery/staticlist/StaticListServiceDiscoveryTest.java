package io.smallrye.discovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.discovery.ServiceDiscovery;
import io.smallrye.discovery.ServiceInstance;

public class StaticListServiceDiscoveryTest {

    private ServiceDiscovery serviceDiscovery;

    @BeforeEach
    void setUp() {
        serviceDiscovery = new ServiceDiscovery();

        Map<String, String> properties = new HashMap<>();
        properties.put("service-discovery.first-service.type", "static");
        properties.put("service-discovery.first-service.1", "http://localhost:8080");
        properties.put("service-discovery.first-service.2", "http://localhost:8081");
        properties.put("service-discovery.second-service.type", "static");
        properties.put("service-discovery.second-service.3", "http://localhost:8082");
        properties.put("service-discovery.third-service.load-balancer", "round-robin");
        properties.put("service-discovery.third-service.type", "kubernetes");
        properties.put("service-discovery.third-service.4", "http://localhost:8083");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();
        new StaticListServiceDiscoveryInitializer(config).init(serviceDiscovery);
    }

    @Test
    void shouldGetAllServiceInstances() {
        List<ServiceInstance> serviceInstances = serviceDiscovery.getAll("first-service")
                .collect()
                .asList()
                .await()
                .indefinitely();

        assertThat(serviceInstances).hasSize(2);
    }

    @Test
    void shouldGetOneServiceInstance() {
        ServiceInstance serviceInstance = serviceDiscovery.get("first-service")
                .await()
                .indefinitely();

        assertThat(serviceInstance.getId()).isEqualTo("1");
        assertThat(serviceInstance.getValue()).isEqualTo("http://localhost:8080");
    }
}
