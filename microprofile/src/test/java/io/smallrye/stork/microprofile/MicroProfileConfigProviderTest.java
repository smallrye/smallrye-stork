package io.smallrye.stork.microprofile;

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
import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.Stork;
import io.smallrye.stork.StorkTestUtils;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.test.TestConfigProvider;
import io.smallrye.stork.test.TestLoadBalancer;
import io.smallrye.stork.test.TestServiceDiscovery;

public class MicroProfileConfigProviderTest {

    public static final String FIRST_SERVICE = "first-service";
    public static final String SECOND_SERVICE = "second-service";
    public static final String THIRD_SERVICE = "third-service";

    private static int initialPriority;

    @BeforeAll
    public static void init() {
        initialPriority = TestConfigProvider.getPriority();
        TestConfigProvider.setPriority(10); // lower it for Stork to pick up the MP one
    }

    @AfterAll
    public static void tearDown() {
        TestConfigProvider.setPriority(initialPriority);
    }

    @Test
    void shouldConfigureServiceDiscoveryOnly() {
        Map<String, String> properties = new HashMap<>();
        properties.put("stork." + FIRST_SERVICE + ".service-discovery", "test-sd-1");
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.1", "http://localhost:8080");
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.2", "http://localhost:8081");

        Stork stork = storkForConfig(properties);

        Assertions.assertThatThrownBy(() -> stork.getService(FIRST_SERVICE).getLoadBalancer())
                .isInstanceOf(IllegalArgumentException.class);

        ServiceDiscovery serviceDiscovery = stork.getService(FIRST_SERVICE).getServiceDiscovery();

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
        properties.put("stork." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer", "test-lb-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("stork." + SECOND_SERVICE + ".service-discovery.3", "http://localhost:8082");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(SECOND_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        ServiceDiscoveryConfig sdConfig = sd.getConfig();
        assertThat(sdConfig.type()).isEqualTo("test-sd-2");
        assertThat(sdConfig.parameters()).hasSize(1);
        assertThat(sdConfig.parameters()).containsAllEntriesOf(Map.of("3", "http://localhost:8082"));

        LoadBalancer loadBalancer = stork.getService(SECOND_SERVICE).getLoadBalancer();
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
        properties.put("stork." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer", "test-lb-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("stork." + SECOND_SERVICE + ".service-discovery.3", "http://localhost:8082");

        properties.put("stork." + THIRD_SERVICE + ".service-discovery", "test-sd-1");
        properties.put("stork." + THIRD_SERVICE + ".load-balancer", "test-lb-1");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(SECOND_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        ServiceDiscoveryConfig sdConfig = sd.getConfig();
        assertThat(sdConfig.type()).isEqualTo("test-sd-2");
        assertThat(sdConfig.parameters()).hasSize(1);
        assertThat(sdConfig.parameters()).containsAllEntriesOf(Map.of("3", "http://localhost:8082"));

        LoadBalancer loadBalancer = stork.getService(SECOND_SERVICE).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer.class);

        TestLoadBalancer lb = (TestLoadBalancer) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
        LoadBalancerConfig lbConfig = lb.getConfig();
        assertThat(lbConfig.type()).isEqualTo("test-lb-2");
        assertThat(lbConfig.parameters())
                .hasSize(1)
                .containsAllEntriesOf(Map.of("some-prop", "some-prop-value"));

        serviceDiscovery = stork.getService(THIRD_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isInstanceOf(TestServiceDiscovery.class);
        sd = (TestServiceDiscovery) serviceDiscovery;

        assertThat(sd.getType()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().type()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().parameters()).isEmpty();

        loadBalancer = stork.getService(THIRD_SERVICE).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer.class);
        lb = (TestLoadBalancer) loadBalancer;

        assertThat(lb.getType()).isEqualTo("test-lb-1");
        assertThat(lb.getConfig().type()).isEqualTo("test-lb-1");
        assertThat(lb.getConfig().parameters()).isEmpty();
    }

    private Stork storkForConfig(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();
        ConfigProviderResolver.setInstance(new TestMicroProfileConfigProvider(config));
        return StorkTestUtils.getNewStorkInstance();
    }
}
