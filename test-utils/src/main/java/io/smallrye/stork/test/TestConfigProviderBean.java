package io.smallrye.stork.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceRegistrarConfig;
import io.smallrye.stork.spi.config.ConfigProvider;

/**
 * Stork config provider for tests, allows easily configuring stuff programmatically.
 * Unlike {@link TestConfigProvider}, this variant is a CDI bean.
 */
@ApplicationScoped
public class TestConfigProviderBean implements ConfigProvider {
    private final List<ServiceConfig> configs = new ArrayList<>();
    private final List<ServiceRegistrarConfig> registrarConfigs = new ArrayList<>();

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
        });
    }

    public void addServiceRegistrarConfig(String registrarName, String registrarType, Map<String, String> parameters) {
        registrarConfigs.add(new ServiceRegistrarConfig() {
            @Override
            public String name() {
                return registrarName;
            }

            @Override
            public String type() {
                return registrarType;
            }

            @Override
            public Map<String, String> parameters() {
                return parameters;
            }
        });
    }

    public void clear() {
        configs.clear();
        registrarConfigs.clear();
    }

    @Override
    public List<ServiceConfig> getConfigs() {
        return configs;
    }

    @Override
    public List<ServiceRegistrarConfig> getRegistrarConfigs() {
        return registrarConfigs;
    }

    @Override
    public int priority() {
        return priority;
    }
}
