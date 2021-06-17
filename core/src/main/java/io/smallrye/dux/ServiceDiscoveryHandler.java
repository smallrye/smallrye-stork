package io.smallrye.dux;

import io.smallrye.mutiny.Multi;

public interface ServiceDiscoveryHandler {

    String getServiceName();

    Multi<ServiceInstance> getServiceInstances();
}
