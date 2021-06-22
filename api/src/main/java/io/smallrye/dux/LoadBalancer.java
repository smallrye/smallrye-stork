package io.smallrye.dux;

import io.smallrye.mutiny.Uni;

/**
 * Works with a single service name.
 * Provides a single service instance.
 * <br>
 * <b>Must be non-blocking</b>
 */
public interface LoadBalancer {

    /**
     * Provide a single {@link ServiceInstance}
     * 
     * @return a Uni with a ServiceInstance
     */
    Uni<ServiceInstance> selectServiceInstance();
}
