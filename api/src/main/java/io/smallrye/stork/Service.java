package io.smallrye.stork;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * Represents a <em>Service</em>.
 * <p>
 * This container gives you access to the service instances and to the selected service instance.
 */
public class Service {

    private final LoadBalancer loadBalancer;
    private final ServiceDiscovery serviceDiscovery;
    private final String serviceName;

    public Service(String serviceName, LoadBalancer loadBalancer, ServiceDiscovery serviceDiscovery) {
        this.loadBalancer = loadBalancer;
        this.serviceDiscovery = serviceDiscovery;
        this.serviceName = serviceName;
    }

    /**
     * Selects a service instance.
     *
     * The selection looks for the service instances and select the one to use using the load balancer.
     *
     * @return a Uni with a ServiceInstance
     */
    public Uni<ServiceInstance> selectServiceInstance() {
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
        if (loadBalancer == null) {
            throw new IllegalArgumentException("No load balancer for service '" + serviceName + "' defined");
        }
        return loadBalancer;
    }

    /**
     * Get the underlying service discovery
     *
     * @return service discovery
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
}
