package io.smallrye.discovery;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

class StaticListServiceDiscovery implements ServiceDiscovery {

    private final List<ServiceInstance> instances;

    StaticListServiceDiscovery(List<ServiceInstance> instances) {
        this.instances = Collections.unmodifiableList(instances);
    }

    @Override
    public CompletionStage<List<ServiceInstance>> getServiceInstances(String name) {
        return CompletableFuture.supplyAsync(() -> getServiceInstancesBlocking(name));
    }

    @Override
    public List<ServiceInstance> getServiceInstancesBlocking(String name) {
        return instances.stream().filter(instance -> instance.getServiceName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }
}
