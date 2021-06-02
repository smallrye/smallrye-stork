package io.smallrye.discovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public final class ServiceDiscovery {

    private final Map<String, ServiceDiscoveryHandler> handlers = new ConcurrentHashMap<>();

    private final Map<String, LoadBalancer> loadBalancers = new ConcurrentHashMap<>();

    public Multi<ServiceInstance> getAll(String serviceName) {
        if (handlers.containsKey(serviceName)) {
            return handlers.get(serviceName).getServiceInstances();
        }

        return Multi.createFrom().empty();
    }

    public Uni<ServiceInstance> get(String serviceName) {
        if (loadBalancers.containsKey(serviceName)) {
            return loadBalancers.get(serviceName).selectServiceInstance(getAll(serviceName));
        }

        return getAll(serviceName).toUni();
    }

    public void registerServiceDiscoveryHandler(ServiceDiscoveryHandler handler) {
        handlers.put(handler.getServiceName(), handler);
    }

    public void registerLoadBalancer(LoadBalancer loadBalancer) {
        loadBalancers.put(loadBalancer.getServiceName(), loadBalancer);
    }
}
