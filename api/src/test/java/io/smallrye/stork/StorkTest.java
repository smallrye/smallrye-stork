package io.smallrye.stork;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.smallrye.stork.config.ConfigProvider;
import io.smallrye.stork.config.LoadBalancerConfig;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.integration.StorkInfrastructure;
import io.smallrye.stork.spi.LoadBalancerProvider;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

/**
 * **IMPORTANT**: Because we mock ServiceLoader, this class cannot use AssertJ.
 */
class StorkTest {

    @SuppressWarnings("rawtypes")
    private MockedStatic<ServiceLoader> loader;

    @AfterEach
    public void cleanup() {
        if (loader != null) {
            loader.close();
        }
        Stork.shutdown();
    }

    @Test
    void initWithoutConfigProvider() {
        Assertions.assertThrows(IllegalStateException.class, Stork::initialize);
    }

    @Test
    void initWithoutServiceDiscoveryOrLoadBalancer() {
        loader = mockStatic(ServiceLoader.class);

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(Collections.emptyList(), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.emptyList());
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class, Collections.emptyList());

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Assertions.assertDoesNotThrow(() -> Stork.initialize());
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertThrows(IllegalArgumentException.class, () -> stork.getService("missing"));
    }

    @Test
    public void initializationWithTwoConfigProviders() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", new FakeServiceDiscoveryConfig(), null);
        ServiceConfig service2 = new FakeServiceConfig("b", new FakeServiceDiscoveryConfig(), null);

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5),
                        new FakeConfigProvider(List.of(service2), 100)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return Mockito.mock(ServiceDiscovery.class);
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class, Collections.emptyList());

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isEmpty());

        Assertions.assertTrue(stork.getServiceOptional("missing").isEmpty());
        Assertions.assertTrue(stork.getServiceOptional("b").isPresent());
        Assertions.assertNotNull(stork.getService("b"));

    }

    @Test
    public void testServiceWithoutServiceDiscovery() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", null, null);

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return Mockito.mock(ServiceDiscovery.class);
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class, Collections.emptyList());

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testServiceWithoutServiceDiscoveryType() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", new FakeServiceDiscoveryConfig() {
            @Override
            public String type() {
                return null;
            }
        }, null);

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return Mockito.mock(ServiceDiscovery.class);
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class, Collections.emptyList());

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testServiceWithServiceDiscoveryButNoMatchingProvider() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", new FakeServiceDiscoveryConfig(), null);

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return Mockito.mock(ServiceDiscovery.class);
                    }

                    @Override
                    public String type() {
                        return "These aren't the droids you're looking for.";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class, Collections.emptyList());

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @Test
    public void testWithLoadBalancer() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", new FakeServiceDiscoveryConfig(), new FakeLoadBalancerConfig());

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return Mockito.mock(ServiceDiscovery.class);
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class,
                Collections.singletonList(new LoadBalancerProvider() {
                    @Override
                    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
                        return null;
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Stork.initialize();
        Stork stork = Stork.getInstance();
        Assertions.assertTrue(stork.getServiceOptional("a").isPresent());

    }

    @Test
    public void testWithLoadBalancerButNoMatchingProvider() {
        loader = mockStatic(ServiceLoader.class);

        ServiceConfig service1 = new FakeServiceConfig("a", new FakeServiceDiscoveryConfig(), new FakeLoadBalancerConfig());

        ServiceLoader<ConfigProvider> configProviders = fakeServiceLoader(ConfigProvider.class,
                List.of(new FakeConfigProvider(List.of(service1), 5)));
        ServiceLoader<ServiceDiscoveryProvider> sdProvider = fakeServiceLoader(ServiceDiscoveryProvider.class,
                Collections.singletonList(new ServiceDiscoveryProvider() {
                    @Override
                    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
                            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
                        return null;
                    }

                    @Override
                    public String type() {
                        return "fake";
                    }
                }));
        ServiceLoader<LoadBalancerProvider> lbProvider = fakeServiceLoader(LoadBalancerProvider.class,
                Collections.singletonList(new LoadBalancerProvider() {
                    @Override
                    public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
                        return null;
                    }

                    @Override
                    public String type() {
                        return "These aren't the droids you're looking for.";
                    }
                }));

        when(ServiceLoader.load(ConfigProvider.class)).thenReturn(configProviders);
        when(ServiceLoader.load(ServiceDiscoveryProvider.class)).thenReturn(sdProvider);
        when(ServiceLoader.load(LoadBalancerProvider.class)).thenReturn(lbProvider);

        Assertions.assertThrows(IllegalArgumentException.class, Stork::initialize);
    }

    @SuppressWarnings("unchecked")
    private <T> ServiceLoader<T> fakeServiceLoader(Class<T> clazz, List<T> providers) {
        ServiceLoader<T> mock = mock(ServiceLoader.class);
        when(ServiceLoader.load(clazz, StorkTest.class.getClassLoader())).thenReturn(mock);
        when(mock.stream()).thenReturn(providers.stream().map(p -> new ServiceLoader.Provider<T>() {
            @Override
            public Class<? extends T> type() {
                return clazz;
            }

            @Override
            public T get() {
                return p;
            }
        }));

        when(mock.iterator()).thenReturn(providers.iterator());
        when(mock.findFirst()).thenReturn(providers.stream().findFirst());
        return mock;
    }

    private static class FakeConfigProvider implements ConfigProvider {

        private final List<ServiceConfig> configs;
        private final int priority;

        public FakeConfigProvider(List<ServiceConfig> configs, int priority) {
            this.configs = configs;
            this.priority = priority;
        }

        @Override
        public List<ServiceConfig> getConfigs() {
            return configs;
        }

        @Override
        public int priority() {
            return priority;
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

    private static class FakeServiceDiscoveryConfig implements ServiceDiscoveryConfig {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    }

    private static class FakeLoadBalancerConfig implements LoadBalancerConfig {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    }
}
