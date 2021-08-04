package io.smallrye.stork.servicediscovery.staticlist;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.DefaultServiceInstance;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;

import java.util.Collections;
import java.util.List;

public final class StaticListServiceDiscovery implements ServiceDiscovery {

    private final List<ServiceInstance> instances;

    public StaticListServiceDiscovery(List<DefaultServiceInstance> instances) {
        this.instances = Collections.unmodifiableList(instances);
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return Uni.createFrom().item(instances);
    }
}
