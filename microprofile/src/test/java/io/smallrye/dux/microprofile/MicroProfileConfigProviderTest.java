package io.smallrye.dux.microprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.dux.Dux;
import io.smallrye.dux.DuxTestUtils;
import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.dux.config.ServiceDiscoveryConfig;
import io.smallrye.dux.test.TestConfigProvider;
import io.smallrye.dux.test.TestLoadBalancer;
import io.smallrye.dux.test.TestServiceDiscovery;

public class MicroProfileConfigProviderTest {

    public static final String FIRST_SERVICE = "first-service";
    public static final String SECOND_SERVICE = "second-service";
    public static final String THIRD_SERVICE = "third-service";

    private static int initialPriority;

    @BeforeAll
    public static void init() {
        initialPriority = TestConfigProvider.getPriority();
        TestConfigProvider.setPriority(10); // lower it for Dux to pick up the MP one
    }

    @AfterAll
    public static void tearDown() {
        TestConfigProvider.setPriority(initialPriority);
    }

    @Test
    void shouldConfigureServiceDiscoveryOnly() {
        Map<String, String> properties = new HashMap<>();
        properties.put("dux." + FIRST_SERVICE + ".service-discovery", "test-sd-1");
        properties.put("dux." + FIRST_SERVICE + ".service-discovery.1", "http://localhost:8080");
        properties.put("dux." + FIRST_SERVICE + ".service-discovery.2", "http://localhost:8081");

        Dux dux = duxForConfig(properties);

        Assertions.assertThatThrownBy(() -> dux.getLoadBalancer(FIRST_SERVICE)).isInstanceOf(IllegalArgumentException.class);

        ServiceDiscovery serviceDiscovery = dux.getServiceDiscovery(FIRST_SERVICE);

        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().type()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().parameters()).hasSize(2)
                .containsAllEntriesOf(Map.of("1", "http://localhost:8080",
                        "2", "http://localhost:8081"));
    }

    @Test
    void shouldConfigureServiceDiscoveryAndLoadBalancer() {
        Map<String, String> properties = new HashMap<>();
        properties.put("dux." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("dux." + SECOND_SERVICE + ".load-balancer", "test-lb-2");
        properties.put("dux." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("dux." + SECOND_SERVICE + ".service-discovery.3", "http://localhost:8082");

        Dux dux = duxForConfig(properties);

        ServiceDiscovery serviceDiscovery = dux.getServiceDiscovery(SECOND_SERVICE);
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        ServiceDiscoveryConfig sdConfig = sd.getConfig();
        assertThat(sdConfig.type()).isEqualTo("test-sd-2");
        assertThat(sdConfig.parameters()).hasSize(1);
        assertThat(sdConfig.parameters()).containsAllEntriesOf(Map.of("3", "http://localhost:8082"));

        LoadBalancer loadBalancer = dux.getLoadBalancer(SECOND_SERVICE);
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer.class);

        TestLoadBalancer lb = (TestLoadBalancer) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
        LoadBalancerConfig lbConfig = lb.getConfig();
        assertThat(lbConfig.type()).isEqualTo("test-lb-2");
        assertThat(lbConfig.parameters())
                .hasSize(1)
                .containsAllEntriesOf(Map.of("some-prop", "some-prop-value"));
    }

    @Test
    void shouldHandleMultipleServices() {
        Map<String, String> properties = new HashMap<>();
        properties.put("dux." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("dux." + SECOND_SERVICE + ".load-balancer", "test-lb-2");
        properties.put("dux." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("dux." + SECOND_SERVICE + ".service-discovery.3", "http://localhost:8082");

        properties.put("dux." + THIRD_SERVICE + ".service-discovery", "test-sd-1");
        properties.put("dux." + THIRD_SERVICE + ".load-balancer", "test-lb-1");

        Dux dux = duxForConfig(properties);

        ServiceDiscovery serviceDiscovery = dux.getServiceDiscovery(SECOND_SERVICE);
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        ServiceDiscoveryConfig sdConfig = sd.getConfig();
        assertThat(sdConfig.type()).isEqualTo("test-sd-2");
        assertThat(sdConfig.parameters()).hasSize(1);
        assertThat(sdConfig.parameters()).containsAllEntriesOf(Map.of("3", "http://localhost:8082"));

        LoadBalancer loadBalancer = dux.getLoadBalancer(SECOND_SERVICE);
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer.class);

        TestLoadBalancer lb = (TestLoadBalancer) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
        LoadBalancerConfig lbConfig = lb.getConfig();
        assertThat(lbConfig.type()).isEqualTo("test-lb-2");
        assertThat(lbConfig.parameters())
                .hasSize(1)
                .containsAllEntriesOf(Map.of("some-prop", "some-prop-value"));

        serviceDiscovery = dux.getServiceDiscovery(THIRD_SERVICE);
        assertThat(serviceDiscovery).isInstanceOf(TestServiceDiscovery.class);
        sd = (TestServiceDiscovery) serviceDiscovery;

        assertThat(sd.getType()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().type()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().parameters()).isEmpty();

        loadBalancer = dux.getLoadBalancer(THIRD_SERVICE);
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer.class);
        lb = (TestLoadBalancer) loadBalancer;

        assertThat(lb.getType()).isEqualTo("test-lb-1");
        assertThat(lb.getConfig().type()).isEqualTo("test-lb-1");
        assertThat(lb.getConfig().parameters()).isEmpty();
    }

    private Dux duxForConfig(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();
        ConfigProviderResolver.setInstance(new TestMicroProfileConfigProvider(config));
        return DuxTestUtils.getNewDuxInstance();
    }
}
