package io.smallrye.stork.loadbalancer.random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;

public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }

        // Fast track - single service instance
        int size = serviceInstances.size();
        if (size == 1) {
            return serviceInstances.iterator().next();
        }

        List<ServiceInstance> list = new ArrayList<>(serviceInstances);
        return list.get(random.nextInt(size));
    }
}
