package io.smallrye.discovery.staticlist;

import java.util.Collections;
import java.util.List;

import io.smallrye.discovery.ServiceDiscoveryHandler;
import io.smallrye.discovery.ServiceInstance;
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
