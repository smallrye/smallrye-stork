package io.smallrye.discovery.roundrobin;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.discovery.LoadBalancer;
import io.smallrye.discovery.ServiceInstance;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger index = new AtomicInteger();

    private final String serviceName;

    public RoundRobinLoadBalancer(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Uni<ServiceInstance> selectServiceInstance(Multi<ServiceInstance> serviceInstances) {
        return serviceInstances.collect()
                .asList()
                .map(this::select);
    }

    private ServiceInstance select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }

        return instances.get(index.getAndIncrement() % instances.size());
    }
}
