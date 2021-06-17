package io.smallrye.dux;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface LoadBalancer {

    String getServiceName();

    Uni<ServiceInstance> selectServiceInstance(Multi<ServiceInstance> serviceInstances);
}
