package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.spi.config.SimpleServiceConfig;

class SimpleServiceConfigOptionalFieldsTest {

    @Test
    void testServiceConfigAllowsNullOptionalConfigs() {
        String serviceName = "my-service";

        SimpleServiceConfig config = new SimpleServiceConfig.Builder()
                .setServiceName(serviceName)
                .build();

        assertEquals(serviceName, config.serviceName());
        assertNull(config.loadBalancer(), "loadBalancer allowed to be null");
        assertNull(config.serviceDiscovery(), "serviceDiscovery allowed to be null");
        assertNull(config.serviceRegistrar(), "serviceRegistrar allowed to be null");
    }

    @Test
    void testServiceConfigWithOnlyLoadBalancer() {
        SimpleServiceConfig.SimpleLoadBalancerConfig lbConfig = new SimpleServiceConfig.SimpleLoadBalancerConfig("lb",
                java.util.Collections.emptyMap());

        SimpleServiceConfig config = new SimpleServiceConfig.Builder()
                .setServiceName("with-lb")
                .setLoadBalancer(lbConfig)
                .build();

        assertEquals("with-lb", config.serviceName());
        assertEquals(lbConfig, config.loadBalancer());
        assertNull(config.serviceDiscovery());
        assertNull(config.serviceRegistrar());
    }

    @Test
    void testServiceConfigWithOnlyServiceDiscovery() {
        SimpleServiceConfig.SimpleServiceDiscoveryConfig sdConfig = new SimpleServiceConfig.SimpleServiceDiscoveryConfig("sd",
                java.util.Collections.emptyMap());

        SimpleServiceConfig config = new SimpleServiceConfig.Builder()
                .setServiceName("with-sd")
                .setServiceDiscovery(sdConfig)
                .build();

        assertEquals("with-sd", config.serviceName());
        assertEquals(sdConfig, config.serviceDiscovery());
        assertNull(config.loadBalancer());
        assertNull(config.serviceRegistrar());
    }

    @Test
    void testServiceConfigWithOnlyServiceRegistrar() {
        SimpleServiceConfig.SimpleServiceRegistrarConfig srConfig = new SimpleServiceConfig.SimpleServiceRegistrarConfig("sr",
                java.util.Collections.emptyMap());

        SimpleServiceConfig config = new SimpleServiceConfig.Builder()
                .setServiceName("with-sr")
                .setServiceRegistrar(srConfig)
                .build();

        assertEquals("with-sr", config.serviceName());
        assertEquals(srConfig, config.serviceRegistrar());
        assertNull(config.loadBalancer());
        assertNull(config.serviceDiscovery());
    }

    @Test
    void testFailsIfServiceDiscoveryForcedNonNull() {
        SimpleServiceConfig.Builder builder = new SimpleServiceConfig.Builder()
                .setServiceName("my-service");

        try {
            SimpleServiceConfig config = builder.build();
            assertNull(config.serviceDiscovery(), "serviceDiscovery allowed to be null");
        } catch (IllegalArgumentException e) {
            fail("serviceDiscovery allowed to be null");
        }
    }
}
