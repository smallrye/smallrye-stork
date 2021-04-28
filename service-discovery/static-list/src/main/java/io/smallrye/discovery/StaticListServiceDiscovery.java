package io.smallrye.discovery;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class StaticListServiceDiscovery implements ServiceDiscovery {

    private final List<ServiceInstance> instances;

    StaticListServiceDiscovery(List<ServiceInstance> instances) {
        this.instances = Collections.unmodifiableList(instances);
    }

    @Override
    public CompletionStage<List<ServiceInstance>> getServiceInstances() {
        return CompletableFuture.completedStage(instances);
    }

    @Override
    public List<ServiceInstance> getServiceInstancesBlocking() {
        return instances;
    }
}
