package io.smallrye.stork;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * Interface to retrieve the list of all available service instances for a given service.
 */
public interface ServiceDiscovery {
    /**
     * Retrieves the service instances.
     * <p>
     * This retrieval is an asynchronous action, thus, the method returns a {@link Uni}
     *
     * @return all `ServiceInstance`'s for the service
     */
    Uni<List<ServiceInstance>> getServiceInstances();

    /**
     * Optional initialization.
     * This method will be invoked after all service discoveries and load balancers are registered in Stork
     *
     * @param stork the Stork instance
     */
    default void initialize(Stork stork) {
    }
}
