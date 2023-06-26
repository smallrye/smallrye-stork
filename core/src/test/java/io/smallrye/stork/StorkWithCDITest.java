package io.smallrye.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.impl.RoundRobinLoadBalancer;
import io.smallrye.stork.impl.RoundRobinLoadBalancerProvider;
import io.smallrye.stork.spi.config.ConfigProvider;

@SuppressWarnings("unchecked")
public class StorkWithCDITest extends WeldTestBase {

    private static final ConfigWithType FAKE_SERVICE_DISCOVERY_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ConfigWithType FAKE_SECURE_SERVICE_DISCOVERY_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("secure", "true");
        }
    };

    private static final ConfigWithType SERVICE_DISCOVERY_CONFIG_WITH_INVALID_PROVIDER = new ConfigWithType() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ConfigWithType FAKE_LOAD_BALANCER_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake-selector";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ConfigWithType LOAD_BALANCER_WITH_INVALID_PROVIDER = new ConfigWithType() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    @BeforeEach
    public void init() {
        TestEnv.configurations.clear();
    }

    @AfterEach
    public void cleanup() {
        Stork.shutdown();
        close();
        AnchoredServiceDiscoveryProvider.services.clear();
        configurations.clear();
    }

    @Test
    void initWithoutConfigProvider() {
        run();
        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("anything")).isEmpty();
    }

    public static final List<ServiceConfig> configurations = new ArrayList<>();

    @Test
    void initWithoutServiceDiscoveryOrLoadBalancer() {
        weld.addBeanClass(MyConfigProvider.class);
        run();
        Assertions.assertDoesNotThrow(() -> Stork.initialize());
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertThrows(NoSuchServiceDefinitionException.class, () -> stork.getService("missing"));
    }

    @ApplicationScoped
    @Typed(ConfigProvider.class)
    public static class MyConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            return new ArrayList<>(configurations);
        }

        @Override
        public int priority() {
            return 5;
        }
    }

    @Test
    public void initializationWithTwoConfigProviders() {
        weld.addBeanClasses(ServiceAConfigProvider.class, ServiceBConfigProvider.class);
        run();
        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isEmpty());

        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertTrue(stork.getServiceOptional("b").isPresent());
        Assertions.assertNotNull(stork.getService("b"));
    }

    @ApplicationScoped
    public static class ServiceAConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            ServiceConfig service = new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, null, null);
            return List.of(service);
        }

        @Override
        public int priority() {
            return 5;
        }
    }

    @ApplicationScoped
    public static class ServiceBConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            ServiceConfig service = new FakeServiceConfig("b", FAKE_SERVICE_DISCOVERY_CONFIG, null, null);
            return List.of(service);
        }

        @Override
        public int priority() {
            return 100;
        }
    }

    @Test
    public void testServiceConfigWithoutServiceDiscovery() {
        run();
        Stork.initialize();
        assertThatThrownBy(() -> Stork.getInstance().defineIfAbsent("a", ServiceDefinition.of(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testServiceWithoutServiceDiscoveryType() {
        configurations.add(new FakeServiceConfig("a", new ConfigWithType() {
            @Override
            public String type() {
                return null;
            }

            @Override
            public Map<String, String> parameters() {
                return null;
            }
        }, null, null));
        weld.addBeanClass(MyConfigProvider.class);
        run();

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testServiceWithServiceDiscoveryButNoMatchingProvider() {
        configurations.add(new FakeServiceConfig("a", SERVICE_DISCOVERY_CONFIG_WITH_INVALID_PROVIDER, null, null));
        weld.addBeanClass(MyConfigProvider.class);
        run();
        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testWithLoadBalancerButNoMatchingProvider() {
        configurations
                .add(new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, LOAD_BALANCER_WITH_INVALID_PROVIDER, null));
        weld.addBeanClass(MyConfigProvider.class);
        run();
        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testWithServiceDiscoveryAndASingleServiceInstance() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);

        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertNotNull(stork.getService("a").getServiceDiscovery());
        Assertions.assertNotNull(stork.getService("a").getLoadBalancer());
        Assertions.assertEquals(v,
                stork.getService("a").selectInstance().await().indefinitely().getLabels().get("label"));
    }

    @Test
    public void testWithLegacySecureServiceDiscovery() {
        configurations.add(new FakeSecureServiceConfig("s",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);

        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("s").isPresent());
        Assertions.assertNotNull(stork.getService("s").getServiceDiscovery());
        Assertions.assertNotNull(stork.getService("s").getLoadBalancer());
        Assertions.assertTrue(stork.getService("s").selectInstance().await().indefinitely().isSecure());
        Assertions.assertEquals(v,
                stork.getService("s").selectInstance().await().indefinitely().getLabels().get("label"));

    }

    @Test
    public void testWithSecureServiceDiscovery() {
        configurations.add(new FakeServiceConfig("s",
                FAKE_SECURE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);

        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("s").isPresent());
        Assertions.assertNotNull(stork.getService("s").getServiceDiscovery());
        Assertions.assertNotNull(stork.getService("s").getLoadBalancer());
        Assertions.assertTrue(stork.getService("s").selectInstance().await().indefinitely().isSecure());
        Assertions.assertEquals(v,
                stork.getService("s").selectInstance().await().indefinitely().getLabels().get("label"));
    }

    @Test
    public void testWithServiceDiscoveryAndATwoServiceInstances() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);

        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);

        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertNotNull(stork.getService("a").getServiceDiscovery());
        Assertions.assertTrue(stork.getService("a").getInstances().await().indefinitely().contains(instance1));
        Assertions.assertTrue(stork.getService("a").getInstances().await().indefinitely().contains(instance2));
        Assertions.assertNotNull(stork.getService("a").getLoadBalancer());
        Assertions.assertEquals(v, stork.getService("a").selectInstance().await().indefinitely().getLabels().get("label"));
        Assertions.assertEquals(v, stork.getService("a").selectInstance().await().indefinitely().getLabels().get("label"));
    }

    @Test
    public void testWithLoadBalancer() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, FAKE_LOAD_BALANCER_CONFIG, null));
        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);
        weld.addBeanClass(MockLoadBalancerProvider.class);
        weld.addBeanClass(AnchoredServiceDiscoveryProviderLoader.class);
        weld.addBeanClass(MockLoadBalancerProviderLoader.class);
        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertNotNull(stork.getService("a").getLoadBalancer());
    }

    @Test
    public void testWithDefaultLoadBalancer() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        ServiceInstance instance3 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        AnchoredServiceDiscoveryProvider.services.add(instance3);

        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);

        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertEquals(instance1, stork.getService("a").selectInstance().await().indefinitely());
        Assertions.assertEquals(instance2, stork.getService("a").selectInstance().await().indefinitely());
        Assertions.assertEquals(instance3, stork.getService("a").selectInstance().await().indefinitely());
        Assertions.assertEquals(instance1, stork.getService("a").selectInstance().await().indefinitely());
        Assertions.assertEquals(instance2, stork.getService("a").selectInstance().await().indefinitely());
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertTrue(stork.getService("a").getLoadBalancer() instanceof RoundRobinLoadBalancer);
    }

    @Test
    public void testWithExplicitConfigurationOfTheRoundRobinLoadBalancer() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        ServiceInstance instance3 = mock(ServiceInstance.class);
        when(instance1.getId()).thenReturn(1L);
        when(instance2.getId()).thenReturn(2L);
        when(instance3.getId()).thenReturn(3L);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        AnchoredServiceDiscoveryProvider.services.add(instance3);

        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, new ConfigWithType() {
                    @Override
                    public String type() {
                        return RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Collections.emptyMap();
                    }
                }, null));

        weld.addBeanClass(AnchoredServiceDiscoveryProvider.class);
        weld.addBeanClass(MyConfigProvider.class);
        run();

        String v = UUID.randomUUID().toString();
        setDataBeanValue(v);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertEquals(1L, stork.getService("a").selectInstance().await().indefinitely().getId());
        Assertions.assertEquals(2L, stork.getService("a").selectInstance().await().indefinitely().getId());
        Assertions.assertEquals(3L, stork.getService("a").selectInstance().await().indefinitely().getId());
        Assertions.assertEquals(1L, stork.getService("a").selectInstance().await().indefinitely().getId());
        Assertions.assertEquals(2L, stork.getService("a").selectInstance().await().indefinitely().getId());
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertTrue(stork.getService("a").getLoadBalancer() instanceof RoundRobinLoadBalancer);
    }

    private static class FakeSecureServiceConfig extends FakeServiceConfig {

        private FakeSecureServiceConfig(String name, ConfigWithType sd, ConfigWithType lb, ConfigWithType sr) {
            super(name, sd, lb, sr);
        }

        @Override
        public boolean secure() {
            return true;
        }
    }

    private void setDataBeanValue(String v) {
        CDI.current().select(MyDataBean.class).get().set(v);
    }

}
