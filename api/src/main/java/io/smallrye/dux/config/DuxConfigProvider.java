package io.smallrye.dux.config;

import java.util.List;

/**
 * Configuration provider for Service Discovery and Load Balancer
 */
public interface DuxConfigProvider {
    /**
     * Get a list of service configurations, each wrapping a configuration for both
     * ServiceDiscovery and LoadBalancer
     *
     * @return a list of configurations
     */
    List<ServiceConfig> getDuxConfigs();

    /**
     * Priority of the configuration provider.
     * A single DuxConfigProvider is used in an application, the one with the highest priority
     *
     * @return the priority
     */
    int priority();
}
