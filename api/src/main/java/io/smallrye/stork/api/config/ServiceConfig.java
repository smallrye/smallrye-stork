package io.smallrye.stork.api.config;

/**
 * Service configuration, wraps (optional) LoadBalancer configuration, (required) ServiceDiscovery configuration and (optional)
 * ServiceRegistrar configuration
 * for a single service
 */
public interface ServiceConfig {
    /**
     *
     * @return (required) name of the service
     */
    String serviceName();

    /**
     * LoadBalancer configuration or null if the service is meant only to only be mapped to a list of services
     *
     * @return (optional) load balancer configuration
     */
    ConfigWithType loadBalancer();

    /**
     * ServiceDiscovery configuration for the service
     *
     * @return (required) service discovery configuration
     */
    ConfigWithType serviceDiscovery();

    /**
     * ServiceRegistrar configuration for the service
     *
     * @return (required) service registrar configuration
     */
    ConfigWithType serviceRegistrar();

    /**
     * Whether the communication should use a secure connection (e.g. HTTPS)
     *
     * @return true if SSL, TLS, etc. should be used for the communication
     * @deprecated Use the service discovery 'secure' attribute instead
     */
    @Deprecated
    default boolean secure() {
        return false;
    }

}
