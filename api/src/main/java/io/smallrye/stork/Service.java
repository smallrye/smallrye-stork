package io.smallrye.stork;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class Service {

    private final LoadBalancer loadBalancer;
    private final ServiceDiscovery serviceDiscovery;

    public Service(LoadBalancer loadBalancer, ServiceDiscovery serviceDiscovery) {
        this.loadBalancer = loadBalancer;
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * Provide a single {@link ServiceInstance}
     *
     * @return a Uni with a ServiceInstance
     */
    public Uni<ServiceInstance> selectServiceInstance() {
        return serviceDiscovery.getServiceInstances().collect()
                .asList()
                .map(loadBalancer::selectServiceInstance);
    }

    public Multi<ServiceInstance> getServiceInstances() {
        return serviceDiscovery.getServiceInstances();
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
}
