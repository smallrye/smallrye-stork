package io.smallrye.stork.test;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;
import io.smallrye.stork.impl.RoundRobinLoadBalancer;
import io.smallrye.stork.impl.RoundRobinLoadBalancerProvider;
import io.smallrye.stork.spi.config.ConfigProvider;

public class StorkTest {

    static final File SPI_ROOT = new File("target/test-classes/META-INF/services");
    static final List<ServiceConfig> configurations = new ArrayList<>();

    private static final ServiceDiscoveryConfig FAKE_SERVICE_DISCOVERY_CONFIG = new ServiceDiscoveryConfig() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ServiceDiscoveryConfig FAKE_SECURE_SERVICE_DISCOVERY_CONFIG = new ServiceDiscoveryConfig() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("secure", "true");
        }
    };

    private static final ServiceDiscoveryConfig SERVICE_DISCOVERY_CONFIG_WITH_INVALID_PROVIDER = new ServiceDiscoveryConfig() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final LoadBalancerConfig FAKE_LOAD_BALANCER_CONFIG = new LoadBalancerConfig() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final LoadBalancerConfig LOAD_BALANCER_WITH_INVALID_PROVIDER = new LoadBalancerConfig() {

        @Override
        public String type() {
            return "non-existent";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };
    private static Set<Path> createdSpis = new HashSet<>();

    @BeforeEach
    public void init() {
        SPI_ROOT.mkdirs();
        AnchoredServiceDiscoveryProvider.services.clear();
        configurations.clear();
    }

    @AfterEach
    public void cleanup() throws IOException {
        Stork.shutdown();
        clearSPIs();
        AnchoredServiceDiscoveryProvider.services.clear();
        configurations.clear();
    }

    private static void clearSPIs() throws IOException {
        for (Path createdSpi : createdSpis) {
            Files.delete(createdSpi);
        }
        createdSpis.clear();
    }

    @Test
    void initWithoutConfigProvider() {
        Assertions.assertThrows(IllegalStateException.class, Stork::initialize);
    }

    @Test
    void initWithoutServiceDiscoveryOrLoadBalancer() {
        install(ConfigProvider.class, AnchoredConfigProvider.class);
        Assertions.assertDoesNotThrow(() -> Stork.initialize());
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertThrows(IllegalArgumentException.class, () -> stork.getService("missing"));
    }

    @Test
    public void initializationWithTwoConfigProviders() {
        install(ConfigProvider.class, ServiceAConfigProvider.class, ServiceBConfigProvider.class);
        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isEmpty());

        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertTrue(stork.getServiceOptional("b").isPresent());
        Assertions.assertNotNull(stork.getService("b"));
    }

    @Test
    public void testServiceConfigWithoutServiceDiscovery() {
        configurations.add(new FakeServiceConfig("a", null, null));

        Assertions.assertThrows(IllegalStateException.class, Stork::initialize);
    }

    @Test
    public void testServiceWithoutServiceDiscoveryType() {
        configurations.add(new FakeServiceConfig("a", new ServiceDiscoveryConfig() {
            @Override
            public String type() {
                return null;
            }

            @Override
            public Map<String, String> parameters() {
                return null;
            }
        }, null));
        install(ConfigProvider.class, AnchoredConfigProvider.class);

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testServiceWithServiceDiscoveryButNoMatchingProvider() {
        configurations.add(new FakeServiceConfig("a", SERVICE_DISCOVERY_CONFIG_WITH_INVALID_PROVIDER, null));
        install(ConfigProvider.class, AnchoredConfigProvider.class);
        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testWithLoadBalancerButNoMatchingProvider() {
        configurations.add(new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, LOAD_BALANCER_WITH_INVALID_PROVIDER));
        install(ConfigProvider.class, AnchoredConfigProvider.class);
        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testWithServiceDiscoveryAndASingleServiceInstance() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        install(ConfigProvider.class, AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertNotNull(stork.getService("a").getServiceDiscovery());
        Assertions.assertEquals(stork.getService("a").selectInstance().await().indefinitely(), instance);
        Assertions.assertNotNull(stork.getService("a").getLoadBalancer());
    }

    @Test
    public void testWithLegacySecureServiceDiscovery() {
        configurations.add(new FakeSecureServiceConfig("s",
                FAKE_SERVICE_DISCOVERY_CONFIG, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        install(ConfigProvider.class, AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("s").isPresent());
        Assertions.assertNotNull(stork.getService("s").getServiceDiscovery());
        Assertions.assertNotNull(stork.getService("s").getLoadBalancer());
        Assertions.assertTrue(stork.getService("s").selectInstance().await().indefinitely().isSecure());
    }

    @Test
    public void testWithSecureServiceDiscovery() {
        configurations.add(new FakeServiceConfig("s",
                FAKE_SECURE_SERVICE_DISCOVERY_CONFIG, null));
        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        install(ConfigProvider.class, AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("s").isPresent());
        Assertions.assertNotNull(stork.getService("s").getServiceDiscovery());
        Assertions.assertNotNull(stork.getService("s").getLoadBalancer());
        Assertions.assertTrue(stork.getService("s").selectInstance().await().indefinitely().isSecure());
    }

    @Test
    public void testWithServiceDiscoveryAndATwoServiceInstances() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, null));
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);

        install(ConfigProvider.class, AnchoredConfigProvider.class);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());
        Assertions.assertNotNull(stork.getService("a").getServiceDiscovery());
        Assertions.assertTrue(stork.getService("a").getInstances().await().indefinitely().contains(instance1));
        Assertions.assertTrue(stork.getService("a").getInstances().await().indefinitely().contains(instance2));
        Assertions.assertNotNull(stork.getService("a").getLoadBalancer());
    }

    @Test
    public void testWithLoadBalancer() {
        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, FAKE_LOAD_BALANCER_CONFIG));
        install(ConfigProvider.class, AnchoredConfigProvider.class);
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
                FAKE_SERVICE_DISCOVERY_CONFIG, null));

        install(ConfigProvider.class, AnchoredConfigProvider.class);

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
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        AnchoredServiceDiscoveryProvider.services.add(instance3);

        configurations.add(new FakeServiceConfig("a",
                FAKE_SERVICE_DISCOVERY_CONFIG, new LoadBalancerConfig() {
                    @Override
                    public String type() {
                        return RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Collections.emptyMap();
                    }
                }));

        install(ConfigProvider.class, AnchoredConfigProvider.class);

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

    public static class ServiceAConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            ServiceConfig service = new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, null);
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
            ServiceConfig service = new FakeServiceConfig("b", FAKE_SERVICE_DISCOVERY_CONFIG, null);
            return List.of(service);
        }

        @Override
        public int priority() {
            return 100;
        }
    }

    public static class AnchoredConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            return new ArrayList<>(configurations);
        }

        @Override
        public int priority() {
            return 5;
        }
    }

    private static class FakeServiceConfig implements ServiceConfig {

        private final String name;
        private final LoadBalancerConfig lb;
        private final ServiceDiscoveryConfig sd;

        private FakeServiceConfig(String name, ServiceDiscoveryConfig sd, LoadBalancerConfig lb) {
            this.name = name;
            this.lb = lb;
            this.sd = sd;
        }

        @Override
        public String serviceName() {
            return name;
        }

        @Override
        public LoadBalancerConfig loadBalancer() {
            return lb;
        }

        @Override
        public ServiceDiscoveryConfig serviceDiscovery() {
            return sd;
        }

        @Override
        public boolean secure() {
            return false;
        }
    }

    private static class FakeSecureServiceConfig extends FakeServiceConfig {

        private FakeSecureServiceConfig(String name, ServiceDiscoveryConfig sd, LoadBalancerConfig lb) {
            super(name, sd, lb);
        }

        @Override
        public boolean secure() {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void install(Class<T> itf, Class<? extends T>... impls) {
        File out = new File(SPI_ROOT, itf.getName());
        if (out.isFile()) {
            throw new IllegalArgumentException(out.getAbsolutePath() + " does already exist");
        }
        if (impls == null || impls.length == 0) {
            throw new IllegalArgumentException("The list of providers must not be `null` or empty");
        }

        List<String> list = Arrays.stream(impls).map(Class::getName).collect(Collectors.toList());
        try {
            Files.write(out.toPath(), list);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        createdSpis.add(out.toPath());
    }

}
