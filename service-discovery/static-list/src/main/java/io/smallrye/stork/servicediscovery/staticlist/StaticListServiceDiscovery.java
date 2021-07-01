package io.smallrye.stork.servicediscovery.staticlist;

import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;

public final class StaticListServiceDiscovery implements ServiceDiscovery {

    private final List<ServiceInstance> instances;

    public StaticListServiceDiscovery(List<ServiceInstance> instances) {
        this.instances = Collections.unmodifiableList(instances);
    }

    @Override
    public Multi<ServiceInstance> getServiceInstances() {
        return Multi.createFrom().iterable(instances);
    }
}
