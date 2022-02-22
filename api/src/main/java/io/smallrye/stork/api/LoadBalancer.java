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
     * Select a single {@link ServiceInstance} from the given list.
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

    /**
     * LoadBalancers often record information of the calls being made with instances they return, be it inflight requests,
     * response time, etc.
     * <br>
     * These calculations often assume that an operation using a previously selected service instance is marked
     * as started before the next instance selection. This prevents a situation in which multiple parallel invocations
     * of the LoadBalancer return the same service instance (because they have the same input for selection).
     * <br>
     * If the load balancer is insusceptible of this problem, this method should return false.
     *
     * @return true if the selecting service instance should be called atomically with marking the operation as started
     *
     * @see Service#selectInstanceAndRecordStart(boolean)
     * @see Service#selectInstanceAndRecordStart(Collection, boolean)
     */
    default boolean requiresStrictRecording() {
        return true;
    }
}
