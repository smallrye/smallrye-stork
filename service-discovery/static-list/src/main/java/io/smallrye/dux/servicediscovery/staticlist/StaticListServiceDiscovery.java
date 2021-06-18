package io.smallrye.dux.servicediscovery.staticlist;

import java.util.Collections;
import java.util.List;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.mutiny.Multi;

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
