package io.smallrye.stork.test;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;

public class SingleLoadBalancer implements LoadBalancer {

    ServiceInstance instance;

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {

        if (instance == null && !serviceInstances.isEmpty()) {
            instance = serviceInstances.iterator().next();
            return instance;
        }

        if (instance != null) {
            return instance;
        }

        throw new NoServiceInstanceFoundException("nope");
    }
}
