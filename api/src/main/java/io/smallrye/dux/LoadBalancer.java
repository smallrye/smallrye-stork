package io.smallrye.dux;

import io.smallrye.mutiny.Uni;

public interface LoadBalancer {
    Uni<ServiceInstance> selectServiceInstance();
}
