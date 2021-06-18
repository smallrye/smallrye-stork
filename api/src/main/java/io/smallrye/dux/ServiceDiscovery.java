package io.smallrye.dux;

import io.smallrye.mutiny.Multi;

public interface ServiceDiscovery {
    Multi<ServiceInstance> getServiceInstances();
}
