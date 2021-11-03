package io.smallrye.stork.config;

/**
 * Service configuration, wraps both (optional) LoadBalancer configuration and (required) ServiceDiscovery configuration
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
    LoadBalancerConfig loadBalancer();

    /**
     * ServiceDiscovery configuration for the service
     * 
     * @return (required) service discovery configuration
     */
    ServiceDiscoveryConfig serviceDiscovery();

    /**
     * Whether the communication should use a connection (etc. HTTPS)
     * 
     * @return true if SSL, TLS, etc. should be used for the communication
     */
    boolean secure();

}
