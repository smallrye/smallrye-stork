package io.smallrye.stork.utils;

import java.util.Collection;

import io.smallrye.stork.api.ServiceInstance;

/**
 * A set of utility methods around {@link ServiceInstance}.
 */
public class ServiceInstanceUtils {

    /**
     * Finds a matching instance for a given hostname and port
     *
     * @param serviceInstances the list of instances
     * @param hostname the hostname
     * @param port the port
     * @return the found instance or {@code null} if none matches
     */
    public static ServiceInstance findMatching(Collection<ServiceInstance> serviceInstances, String hostname, int port) {
        if (hostname == null) {
            throw new NullPointerException("Hostname cannot be null");
        }
        for (ServiceInstance instance : serviceInstances) {
            if (hostname.equals(instance.getHost()) && port == instance.getPort()) {
                return instance;
            }
        }
        return null;
    }

    private ServiceInstanceUtils() {
        // Avoid direct instantiation.
    }
}
