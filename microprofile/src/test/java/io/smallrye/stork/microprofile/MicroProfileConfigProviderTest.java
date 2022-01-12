package io.smallrye.stork.microprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.impl.RoundRobinLoadBalancer;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.smallrye.stork.test.TestLoadBalancer1;
import io.smallrye.stork.test.TestLoadBalancer2;
import io.smallrye.stork.test.TestServiceDiscovery;
import io.smallrye.stork.test.TestServiceDiscovery2;
import io.smallrye.stork.test.TestServiceDiscovery2ProviderConfiguration;

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
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.one", "http://localhost:8080");
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.two", "http://localhost:8081");

        Stork stork = storkForConfig(properties);

        // Use round-robin when not configured.
        assertThat(stork.getService(FIRST_SERVICE).getLoadBalancer()).isInstanceOf(RoundRobinLoadBalancer.class);

        ServiceDiscovery serviceDiscovery = stork.getService(FIRST_SERVICE).getServiceDiscovery();

        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().getOne()).isEqualTo("http://localhost:8080");
        assertThat(sd.getConfig().getTwo()).isEqualTo("http://localhost:8081");
    }

    @Test
    void shouldConfigureServiceDiscoveryOnlyUsingEmbeddedType() {
        Map<String, String> properties = new HashMap<>();
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.type", "test-sd-1");
        properties.put("stork." + FIRST_SERVICE + ".service-discovery.one", "http://localhost:8080");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(FIRST_SERVICE).getServiceDiscovery();

        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery.class);

        TestServiceDiscovery sd = (TestServiceDiscovery) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-1");
        assertThat(sd.getConfig().getOne()).isEqualTo("http://localhost:8080");
        assertThat(sd.getConfig().getTwo()).isEqualTo(null);
    }

    @Test
    void shouldConfigureServiceDiscoveryAndLoadBalancer() {
        Map<String, String> properties = new HashMap<>();
        properties.put("stork." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer.type", "test-lb-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("stork." + SECOND_SERVICE + ".service-discovery.three", "http://localhost:8082");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(SECOND_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery2.class);

        TestServiceDiscovery2 sd = (TestServiceDiscovery2) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        TestServiceDiscovery2ProviderConfiguration sdConfig = sd.getConfig();
        assertThat(sdConfig.getThree()).isEqualTo("http://localhost:8082");

        LoadBalancer loadBalancer = stork.getService(SECOND_SERVICE).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer2.class);

        TestLoadBalancer2 lb = (TestLoadBalancer2) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
        var lbConfig = lb.getConfig();

        assertThat(lbConfig.getSomeProp()).isEqualTo("some-prop-value");
    }

    @Test
    void shouldHandleMultipleServices() {
        Map<String, String> properties = new HashMap<>();
        properties.put("stork." + SECOND_SERVICE + ".service-discovery", "test-sd-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer", "test-lb-2");
        properties.put("stork." + SECOND_SERVICE + ".load-balancer.some-prop", "some-prop-value");
        properties.put("stork." + SECOND_SERVICE + ".service-discovery.three", "http://localhost:8082");

        properties.put("stork." + THIRD_SERVICE + ".service-discovery", "test-sd-1");
        properties.put("stork." + THIRD_SERVICE + ".load-balancer", "test-lb-1");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(SECOND_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery2.class);

        TestServiceDiscovery2 sd = (TestServiceDiscovery2) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        TestServiceDiscovery2ProviderConfiguration sdConfig = sd.getConfig();
        assertThat(sdConfig.getThree()).isEqualTo("http://localhost:8082");

        LoadBalancer loadBalancer = stork.getService(SECOND_SERVICE).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer2.class);

        TestLoadBalancer2 lb = (TestLoadBalancer2) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
        var lbConfig = lb.getConfig();
        assertThat(lbConfig.getSomeProp()).isEqualTo("some-prop-value");

        serviceDiscovery = stork.getService(THIRD_SERVICE).getServiceDiscovery();
        assertThat(serviceDiscovery).isInstanceOf(TestServiceDiscovery.class);
        TestServiceDiscovery sd3 = (TestServiceDiscovery) serviceDiscovery;

        assertThat(sd3.getType()).isEqualTo("test-sd-1");
        assertThat(sd3.getConfig().getOne()).isNull();
        assertThat(sd3.getConfig().getTwo()).isNull();

        loadBalancer = stork.getService(THIRD_SERVICE).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer1.class);
        TestLoadBalancer1 lb1 = (TestLoadBalancer1) loadBalancer;

        assertThat(lb1.getType()).isEqualTo("test-lb-1");
    }

    @Test
    void shouldHandleServiceNamesInQuotes() {
        String serviceName = "my.service";

        Map<String, String> properties = new HashMap<>();
        properties.put("stork.\"my.service\".service-discovery", "test-sd-2");
        properties.put("stork.\"my.service\".load-balancer", "test-lb-2");
        properties.put("stork.\"my.service\".service-discovery.three", "http://localhost:8082");

        Stork stork = storkForConfig(properties);

        ServiceDiscovery serviceDiscovery = stork.getService(serviceName).getServiceDiscovery();
        assertThat(serviceDiscovery).isNotNull().isInstanceOf(TestServiceDiscovery2.class);

        TestServiceDiscovery2 sd = (TestServiceDiscovery2) serviceDiscovery;
        assertThat(sd.getType()).isEqualTo("test-sd-2");

        TestServiceDiscovery2ProviderConfiguration sdConfig = sd.getConfig();
        assertThat(sdConfig.getThree()).isEqualTo("http://localhost:8082");

        LoadBalancer loadBalancer = stork.getService(serviceName).getLoadBalancer();
        assertThat(loadBalancer).isInstanceOf(TestLoadBalancer2.class);

        TestLoadBalancer2 lb = (TestLoadBalancer2) loadBalancer;

        assertThat(lb.getServiceDiscovery()).isEqualTo(serviceDiscovery);
        assertThat(lb.getType()).isEqualTo("test-lb-2");
    }

    private Stork storkForConfig(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigValuePropertiesConfigSource(properties, "test-config-source", 0))
                .build();
        ConfigProviderResolver.setInstance(new TestMicroProfileConfigProvider(config));
        return StorkTestUtils.getNewStorkInstance();
    }
}
