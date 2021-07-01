package io.smallrye.dux.loadbalancer.roundrobin;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.dux.LoadBalancer;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.mutiny.Uni;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger index = new AtomicInteger();

    private final ServiceDiscovery serviceDiscovery;

    public RoundRobinLoadBalancer(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public Uni<ServiceInstance> selectServiceInstance() {
        return serviceDiscovery.getServiceInstances().collect()
                .asList()
                .map(this::select);
    }

    @Override
    public ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances) {
        serviceInstances.sort(Comparator.comparingLong(ServiceInstance::getId));
        return select(serviceInstances);
    }

    private ServiceInstance select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }

        return instances.get(index.getAndIncrement() % instances.size());
    }
}
