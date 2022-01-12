package io.smallrye.stork.api;

import java.util.Collection;

/**
 * Works with a single service name.
 * Provides a single service instance.
 * <br>
 * <b>Must be non-blocking</b>
 */
public interface LoadBalancer {

    /**
     * Provide a single {@link ServiceInstance} from the given list.
     *
     * @param serviceInstances instances to choose from
     * 
     * @return a ServiceInstance
     *
     * @throws NoServiceInstanceFoundException if the incoming collection is empty or all the service instances in the
     *         collection
     *         are deemed invalid for some reason
     */
    ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances);
}
