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
     * Provide a single {@link DefaultServiceInstance} from the given list.
     *
     * @param serviceInstances instances to choose from
     * @return a ServiceInstance
     */
    ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances);
}
