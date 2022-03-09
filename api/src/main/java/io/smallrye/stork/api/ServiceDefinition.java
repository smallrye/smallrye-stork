package io.smallrye.stork.api;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;

/**
 * Define a service that will be managed by Stork.
 */
public class ServiceDefinition {
    private final LoadBalancerConfig loadBalancer;
    private final ServiceDiscoveryConfig serviceDiscovery;

    private ServiceDefinition(ServiceDiscoveryConfig sd, LoadBalancerConfig lb) {
        serviceDiscovery = ParameterValidation.nonNull(sd, "service discovery config");
        loadBalancer = lb;
    }

    /**
     * Creates a new {@link ServiceDefinition} using the given {@link ServiceDiscoveryConfig}.
     *
     * @param sd the service discovery config, must not be {@code null}
     * @return the created service definition
     */
    public static ServiceDefinition of(ServiceDiscoveryConfig sd) {
        return of(sd, null);
    }

    /**
     * Creates a new {@link ServiceDefinition} using the given {@link ServiceDiscoveryConfig} and {@link LoadBalancerConfig}.
     *
     * @param sd the service discovery config, must not be {@code null}
     * @param lb the load balancer config, if {@code null}, round-robin is used.
     * @return the created service definition
     */
    public static ServiceDefinition of(ServiceDiscoveryConfig sd, LoadBalancerConfig lb) {
        return new ServiceDefinition(sd, lb);
    }

    /**
     * @return the configured load balancer config.
     */
    public LoadBalancerConfig getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * @return the configured service discovery config.
     */
    public ServiceDiscoveryConfig getServiceDiscovery() {
        return serviceDiscovery;
    }
}
