package io.smallrye.dux.servicediscovery.staticlist;

import java.util.Collections;
import java.util.List;

import io.smallrye.dux.ServiceDiscoveryHandler;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.mutiny.Multi;

public final class StaticListServiceDiscoveryHandler implements ServiceDiscoveryHandler {

    private final String serviceName;

    private final List<ServiceInstance> instances;

    public StaticListServiceDiscoveryHandler(String serviceName, List<ServiceInstance> instances) {
        this.serviceName = serviceName;
        this.instances = Collections.unmodifiableList(instances);
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public Multi<ServiceInstance> getServiceInstances() {
        return Multi.createFrom().iterable(instances);
    }
}
