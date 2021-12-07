package io.smallrye.stork;

import java.util.List;
import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Represents a <em>Service</em>.
 * <p>
 * This container gives you access to the service instances and to the selected service instance.
 */
public class Service {

    private final Optional<LoadBalancer> loadBalancer;
    private final ServiceDiscovery serviceDiscovery;
    private final String serviceName;
    private final boolean secure;

    public Service(String serviceName, Optional<LoadBalancer> loadBalancer, ServiceDiscovery serviceDiscovery, boolean secure) {
        this.loadBalancer = loadBalancer;
        this.serviceDiscovery = serviceDiscovery;
        this.serviceName = serviceName;
        this.secure = secure;
    }

    /**
     * Selects a service instance.
     *
     * The selection looks for the service instances and select the one to use using the load balancer.
     *
     * @return a Uni with a ServiceInstance, or with {@link NoServiceInstanceFoundException} if the load balancer failed to find
     *         a service instance capable of handling a call
     * @throws IllegalArgumentException if the current service does not use a load balancer
     */
    public Uni<ServiceInstance> selectServiceInstance() {
        LoadBalancer loadBalancer = getLoadBalancer();
        return serviceDiscovery.getServiceInstances()
                .map(loadBalancer::selectServiceInstance);
    }

    /**
     * Provide a collection of {@link DefaultServiceInstance}s
     *
     * @return a Uni producing the list of {@link ServiceInstance ServiceInstances}.
     */
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return serviceDiscovery.getServiceInstances();
    }

    /**
     * Get the underlying load balancer instance.
     *
     * @return load balancer
     * @throws IllegalArgumentException if the current service does not use a load balancer.
     */
    public LoadBalancer getLoadBalancer() {
        return loadBalancer
                .orElseThrow(() -> new IllegalArgumentException("No load balancer for service '" + serviceName + "' defined"));
    }

    /**
     * Get the underlying service discovery
     *
     * @return service discovery
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public boolean isSecure() {
        return secure;
    }
}
