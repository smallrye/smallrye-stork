package io.smallrye.stork;

import java.util.List;

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
     * This is useful for cases like gRPC where connections to all available service instances
     * may be created upfront, and then, per call, one of them is selected.
     *
     * @param serviceInstances instances to choose from
     * @return a ServiceInstance
     */
    ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances);
}
