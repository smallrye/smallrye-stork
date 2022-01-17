package io.smallrye.stork.servicediscovery.staticlist;

import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.DefaultServiceInstance;

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
