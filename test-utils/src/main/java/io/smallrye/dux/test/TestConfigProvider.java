package io.smallrye.dux.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.dux.config.DuxConfigProvider;
import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.dux.config.ServiceConfig;
import io.smallrye.dux.config.ServiceDiscoveryConfig;

/**
 * Dux config provider for tests, allows easily configuring stuff programmatically
 */
public class TestConfigProvider implements DuxConfigProvider {
    private static final List<ServiceConfig> configs = new ArrayList<>();

    public static void addServiceConfig(String name, String loadBalancer, String serviceDiscovery,
            Map<String, String> loadBalancerParams, Map<String, String> serviceDiscoveryParams) {
        configs.add(new ServiceConfig() {
            @Override
            public String serviceName() {
                return name;
            }

            @Override
            public LoadBalancerConfig loadBalancer() {
                return loadBalancer == null ? null : new LoadBalancerConfig() {
                    @Override
                    public String type() {
                        return loadBalancer;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return loadBalancerParams;
                    }
                };
            }

            @Override
            public ServiceDiscoveryConfig serviceDiscovery() {
                return new ServiceDiscoveryConfig() {
                    @Override
                    public String type() {
                        return serviceDiscovery;
                    }

                    @Override
                    public Map<String, String> parameters() {
                        return serviceDiscoveryParams;
                    }
                };
            }
        });
    }

    public static void clear() {
        configs.clear();
    }

    @Override
    public List<ServiceConfig> getDuxConfigs() {
        return configs;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE; // make sure this one is selected
    }
}
