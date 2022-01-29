package io.smallrye.stork.loadbalancer.requests;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoAcceptableServiceInstanceFoundException;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;

public class LeastRequestsLoadBalancer implements LoadBalancer {

    private final InflightRequestCollector collector = new InflightRequestCollector();

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }

        ServiceInstance selected = null;
        int min = Integer.MAX_VALUE;
        for (ServiceInstance instance : serviceInstances) {
            int concurrency = collector.get(instance.getId());
            if (concurrency < min) {
                selected = instance;
                min = concurrency;
            }
        }

        if (selected == null) {
            throw new NoAcceptableServiceInstanceFoundException("No service instance found");
        }

        collector.selected(selected.getId());
        return new ServiceInstanceWithStatGathering(selected, collector);
    }
}
