package io.smallrye.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.access.ServiceConfigAccessor;

public class StaticListServiceDiscoveryTest {

    private ServiceDiscoveryProducer serviceDiscoveryProducer;

    @BeforeEach
    void setUp() {
        Map<String, String> properties = new HashMap<>();
        properties.put("service-discovery.first-service.type", "static");
        properties.put("service-discovery.first-service.1", "http://localhost:8080");
        properties.put("service-discovery.first-service.2", "http://localhost:8081");
        properties.put("service-discovery.second-service.type", "static");
        properties.put("service-discovery.second-service.3", "http://localhost:8082");
        properties.put("service-discovery.third-service.type", "kubernetes");
        properties.put("service-discovery.third-service.4", "http://localhost:8083");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();

        serviceDiscoveryProducer = new StaticListServiceDiscoveryProducer(
                new ServiceConfigAccessor(config, StaticListServiceDiscoveryProducer.CONFIG_PREFIX));
    }

    @Test
    void shouldGetServiceDiscoveryWithMultipleInstances() {
        ServiceDiscovery serviceDiscovery = serviceDiscoveryProducer.getServiceDiscovery("first-service");

        assertThat(serviceDiscovery.getServiceInstancesBlocking()).hasSize(2);
    }

    @Test
    void shouldGetServiceDiscoveryWithSingleInstance() {
        ServiceDiscovery serviceDiscovery = serviceDiscoveryProducer.getServiceDiscovery("second-service");
        List<ServiceInstance> serviceInstances = serviceDiscovery.getServiceInstancesBlocking();

        assertThat(serviceInstances).hasSize(1);
        assertThat(serviceInstances.get(0).getId()).isEqualTo("3");
        assertThat(serviceInstances.get(0).getValue()).isEqualTo("http://localhost:8082");
    }

    @Test
    void shouldNotGetWronglyConfiguredServiceDiscovery() {
        assertThatThrownBy(() -> serviceDiscoveryProducer.getServiceDiscovery("third-service")).isInstanceOf(
                IllegalArgumentException.class);
    }
}
