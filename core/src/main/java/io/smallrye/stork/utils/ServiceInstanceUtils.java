package io.smallrye.stork.utils;

import java.util.Collection;

import io.smallrye.stork.api.ServiceInstance;

public class ServiceInstanceUtils {

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

    }
}
