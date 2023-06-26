package io.smallrye.stork.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;

/**
 * Stork config provider for tests, allows easily configuring stuff programmatically.
 * Unlike {@link TestConfigProvider}, this variant is a CDI bean.
 */
@ApplicationScoped
public class TestConfigProviderBean implements ConfigProvider {
    private final List<ServiceConfig> configs = new ArrayList<>();
    private int priority = Integer.MAX_VALUE;

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void addServiceConfig(String name, String loadBalancer, String serviceDiscovery,
            Map<String, String> loadBalancerParams, Map<String, String> serviceDiscoveryParams) {
        configs.add(new ServiceConfig() {
            @Override
            public String serviceName() {
                return name;
            }

            @Override
            public ConfigWithType loadBalancer() {
                return loadBalancer == null ? null : new ConfigWithType() {
                    @Override
                    public String type() {
                        return loadBalancer;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Objects.requireNonNullElse(loadBalancerParams, Collections.emptyMap());
                    }
                };
            }

            @Override
            public ConfigWithType serviceDiscovery() {
                return new ConfigWithType() {
                    @Override
                    public String type() {
                        return serviceDiscovery;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Objects.requireNonNullElse(serviceDiscoveryParams, Collections.emptyMap());
                    }
                };
            }

            @Override
            public ConfigWithType serviceRegistrar() {
                return null;
            }
        });
    }

    public void addServiceConfig(String name, String loadBalancer, String serviceDiscovery, String serviceRegistrar,
            Map<String, String> loadBalancerParams, Map<String, String> serviceDiscoveryParams,
            Map<String, String> serviceRegistrarParams) {
        configs.add(new ServiceConfig() {
            @Override
            public String serviceName() {
                return name;
            }

            @Override
            public ConfigWithType loadBalancer() {
                return loadBalancer == null ? null : new ConfigWithType() {
                    @Override
                    public String type() {
                        return loadBalancer;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Objects.requireNonNullElse(loadBalancerParams, Collections.emptyMap());
                    }
                };
            }

            @Override
            public ConfigWithType serviceDiscovery() {
                return new ConfigWithType() {
                    @Override
                    public String type() {
                        return serviceDiscovery;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Objects.requireNonNullElse(serviceDiscoveryParams, Collections.emptyMap());
                    }
                };
            }

            @Override
            public ConfigWithType serviceRegistrar() {
                return serviceRegistrar == null ? null : new ConfigWithType() {
                    @Override
                    public String type() {
                        return serviceRegistrar;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return Objects.requireNonNullElse(serviceRegistrarParams, Collections.emptyMap());
                    }
                };
            }
        });
    }

    public void clear() {
        configs.clear();
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
