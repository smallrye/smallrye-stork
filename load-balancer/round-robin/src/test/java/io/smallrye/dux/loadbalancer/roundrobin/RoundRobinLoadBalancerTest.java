package io.smallrye.dux.loadbalancer.roundrobin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceDiscoveryHandler;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.mutiny.Multi;

public class RoundRobinLoadBalancerTest {

    private final List<ServiceInstance> serviceInstances = Arrays.asList(
            new ServiceInstance("1", "first"),
            new ServiceInstance("2", "second"));

    private ServiceDiscovery serviceDiscovery;

    @BeforeEach
    void setUp() {
        serviceDiscovery = new ServiceDiscovery();

        Map<String, String> properties = new HashMap<>();
        properties.put("load-balancer.first-service.type", "round-robin");
        properties.put("load-balancer.second-service.type", "round-robin");
        properties.put("load-balancer.third-service.type", "something-else");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();
        new RoundRobinLoadBalancerInitializer(config).init(serviceDiscovery);

        serviceDiscovery.registerServiceDiscoveryHandler(new ServiceDiscoveryHandler() {
            @Override
            public String getServiceName() {
                return "first-service";
            }

            @Override
            public Multi<ServiceInstance> getServiceInstances() {
                return Multi.createFrom().iterable(serviceInstances);
            }
        });
    }

    @Test
    public void shouldGetServiceInstance() {
        assertThat(serviceDiscovery.get("first-service").await().indefinitely()).isEqualTo(serviceInstances.get(0));
        assertThat(serviceDiscovery.get("first-service").await().indefinitely()).isEqualTo(serviceInstances.get(1));
        assertThat(serviceDiscovery.get("first-service").await().indefinitely()).isEqualTo(serviceInstances.get(0));
    }
}
