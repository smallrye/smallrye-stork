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
}
