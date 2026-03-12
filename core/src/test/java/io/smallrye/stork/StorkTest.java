package io.smallrye.stork;

import static io.smallrye.stork.FakeServiceConfig.FAKE_LOAD_BALANCER_CONFIG;
import static io.smallrye.stork.FakeServiceConfig.FAKE_SECURE_SERVICE_DISCOVERY_CONFIG;
import static io.smallrye.stork.FakeServiceConfig.FAKE_SERVICE_DISCOVERY_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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
public class StorkTest {

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

    private static final ConfigWithType SERVICE_REGISTRAR_WITH_INVALID_PROVIDER = new ConfigWithType() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ConfigWithType DISABLED_UNKNOWN_SERVICE_REGISTRAR_PROVIDER = new ConfigWithType() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("enabled", "false");
        }
    };

    private static final ConfigWithType SERVICE_REGISTRAR_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "test";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    @BeforeEach
    public void init() {
        TestEnv.SPI_ROOT.mkdirs();
        AnchoredServiceDiscoveryProvider.services.clear();
        TestEnv.configurations.clear();
    }

    @AfterEach
    public void cleanup() throws IOException {
        Stork.shutdown();
        TestEnv.clearSPIs();
        AnchoredServiceDiscoveryProvider.services.clear();
        TestEnv.configurations.clear();
    }

    @Test
    void initWithoutConfigProvider() {
        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("anything")).isEmpty();
    }

    @Test
    void initWithoutServiceDiscoveryOrLoadBalancer() {
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatCode(() -> Stork.initialize()).doesNotThrowAnyException();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("missing")).isEmpty();
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(NoSuchServiceDefinitionException.class);
    }

    @Test
    public void initializationWithTwoConfigProviders() {
        TestEnv.install(ConfigProvider.class, ServiceAConfigProvider.class, ServiceBConfigProvider.class);
        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("a")).isEmpty();

        assertThat(stork.getServiceOptional("missing")).isEmpty();
        assertThat(stork.getServiceOptional("b")).isPresent();
        assertThat(stork.getService("b")).isNotNull();
    }

    @Test
    public void testServiceConfigWithoutServiceDiscovery() {
        Stork.initialize();
        assertThatThrownBy(() -> Stork.getInstance().defineIfAbsent("a", ServiceDefinition.of(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testServiceWithoutServiceDiscoveryType() {
        TestEnv.configurations.add(new FakeServiceConfig("a", new ConfigWithType() {
            @Override
            public String type() {
                return null;
            }

            @Override
            public Map<String, String> parameters() {
                return null;
            }
        }, null, null));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        assertThatThrownBy(Stork::initialize).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testServiceWithServiceDiscoveryButNoMatchingProvider() {
        TestEnv.configurations.add(new FakeServiceConfig("a", SERVICE_DISCOVERY_CONFIG_WITH_INVALID_PROVIDER, null, null));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatThrownBy(Stork::initialize).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWithLoadBalancerButNoMatchingProvider() {
        TestEnv.configurations
                .add(new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, LOAD_BALANCER_WITH_INVALID_PROVIDER, null));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatThrownBy(Stork::initialize).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWithRegistrarButNoMatchingProvider() {
        TestEnv.configurations
                .add(new FakeServiceConfig("a", null, null, SERVICE_REGISTRAR_WITH_INVALID_PROVIDER));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatThrownBy(Stork::initialize).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWithDisabledUnknownRegistrarNoMatchingProvider() {
        TestEnv.configurations
                .add(new FakeServiceConfig("a", null, null, DISABLED_UNKNOWN_SERVICE_REGISTRAR_PROVIDER));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatCode(() -> Stork.initialize()).doesNotThrowAnyException();
    }

    @Test
    public void testWithServiceDiscoveryAndASingleServiceInstance() {
        TestEnv.configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("a")).isPresent();
        assertThat(stork.getService("a").getServiceDiscovery()).isNotNull();
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance);
        assertThat(stork.getService("a").getLoadBalancer()).isNotNull();
    }

    @Test
    public void testWithLegacySecureServiceDiscovery() {
        TestEnv.configurations.add(new FakeSecureServiceConfig("s",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("s")).isPresent();
        assertThat(stork.getService("s").getServiceDiscovery()).isNotNull();
        assertThat(stork.getService("s").getLoadBalancer()).isNotNull();
        assertThat(stork.getService("s").selectInstance().await().indefinitely().isSecure()).isTrue();
    }

    @Test
    public void testWithSecureServiceDiscovery() {
        TestEnv.configurations.add(new FakeServiceConfig("s",
                FAKE_SECURE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("s")).isPresent();
        assertThat(stork.getService("s").getServiceDiscovery()).isNotNull();
        assertThat(stork.getService("s").getLoadBalancer()).isNotNull();
        assertThat(stork.getService("s").selectInstance().await().indefinitely().isSecure()).isTrue();
    }

    @Test
    public void testWithServiceDiscoveryAndATwoServiceInstances() {
        TestEnv.configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("a")).isPresent();
        assertThat(stork.getService("a").getServiceDiscovery()).isNotNull();
        assertThat(stork.getService("a").getInstances().await().indefinitely()).contains(instance1, instance2);
        assertThat(stork.getService("a").getLoadBalancer()).isNotNull();
    }

    @Test
    public void testWithLoadBalancer() {
        TestEnv.configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, FAKE_LOAD_BALANCER_CONFIG, null));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("a")).isPresent();
        assertThat(stork.getService("a").getLoadBalancer()).isNotNull();
    }

    @Test
    public void testWithDefaultLoadBalancer() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        ServiceInstance instance3 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        AnchoredServiceDiscoveryProvider.services.add(instance3);

        TestEnv.configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance1);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance2);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance3);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance1);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance2);
        assertThat(stork.getServiceOptional("a")).isPresent();
        assertThat(stork.getService("a").getLoadBalancer()).isInstanceOf(RoundRobinLoadBalancer.class);
    }

    @Test
    public void testWithExplicitConfigurationOfTheRoundRobinLoadBalancer() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        ServiceInstance instance3 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        AnchoredServiceDiscoveryProvider.services.add(instance3);

        TestEnv.configurations.add(new FakeServiceConfig("a",
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

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance1);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance2);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance3);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance1);
        assertThat(stork.getService("a").selectInstance().await().indefinitely()).isEqualTo(instance2);
        assertThat(stork.getServiceOptional("a")).isPresent();
        assertThat(stork.getService("a").getLoadBalancer()).isInstanceOf(RoundRobinLoadBalancer.class);
    }

    @Test
    public void initWithOnlyServiceRegistrarConfiguration() {
        TestEnv.configurations.add(new FakeServiceConfig("test", null, null, SERVICE_REGISTRAR_CONFIG));
        TestEnv.install(ConfigProvider.class, RegistrarConfigProvider.class);
        assertThatCode(() -> Stork.initialize()).doesNotThrowAnyException();
    }

    @Test
    void initWithoutServiceDiscovery() {
        TestEnv.configurations.add(new FakeServiceConfig("a", null, null, null));
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        assertThatCode(() -> Stork.initialize()).doesNotThrowAnyException();
        Stork stork = Stork.getInstance();
        assertThat(stork.getServiceOptional("missing")).isEmpty();
        assertThatThrownBy(() -> stork.getService("missing")).isInstanceOf(NoSuchServiceDefinitionException.class);
    }

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

    public static class RegistrarConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            ServiceConfig service = new FakeServiceConfig("test", null, null, SERVICE_REGISTRAR_CONFIG);
            return List.of(service);
        }

        @Override
        public int priority() {
            return 100;
        }
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

}
