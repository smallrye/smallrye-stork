package io.smallrye.stork.spi.config;

import java.util.List;

import io.smallrye.stork.api.config.ServiceConfig;

/**
 * Configuration provider for Service Discovery and Load Balancer
 */
public interface ConfigProvider {
    /**
     * Get a list of service configurations, each wrapping a configuration for both
     * ServiceDiscovery and LoadBalancer
     *
     * @return a list of configurations
     */
    List<ServiceConfig> getConfigs();

    /**
     * Priority of the configuration provider.
     * A single ConfigProvider is used in an application, the one with the highest priority
     *
     * @return the priority
     */
    int priority();
}
