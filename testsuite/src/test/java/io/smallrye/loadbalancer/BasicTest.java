package io.smallrye.loadbalancer;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class BasicTest {
    @Test
    void shouldGiveUri() {
        Map<String, String> properties = new HashMap<>();
        properties.put("io.smallrye.loadbalancer.type", "round-robin");
        properties.put("io.smallrye.loadbalancer.address-provider", "static");
        properties.put("io.smallrye.loadbalancer.addresses", "https://example.com:8182,localhost:9092");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();


        ReflectionLoadBalancerFactory producerFactory = new ReflectionLoadBalancerFactory();
        // TODO
    }
}
