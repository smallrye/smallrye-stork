package io.smallrye.loadbalancer.roundrobin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.discovery.ServiceDiscovery;
import io.smallrye.discovery.ServiceInstance;
import io.smallrye.loadbalancer.LoadBalancer;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger index = new AtomicInteger();

    private final ServiceDiscovery serviceDiscovery;

    public RoundRobinLoadBalancer(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public CompletionStage<ServiceInstance> getServiceInstance() {
        return this.serviceDiscovery.getServiceInstances()
                .thenComposeAsync((instances) -> CompletableFuture.completedStage(selectInstance(instances)));
    }

    @Override
    public Optional<ServiceInstance> getServiceInstanceBlocking() {
        return Optional.ofNullable(selectInstance(serviceDiscovery.getServiceInstancesBlocking()));
    }

    @Override
    public void registerExecution(ServiceInstance instance, long executionTimeMillis, Throwable error) {
        // Noop
    }

    private ServiceInstance selectInstance(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }

        return instances.get(index.getAndIncrement() % instances.size());
    }
}
