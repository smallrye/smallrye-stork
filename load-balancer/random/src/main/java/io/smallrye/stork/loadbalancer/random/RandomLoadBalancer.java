package io.smallrye.stork.loadbalancer.random;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;

/**
 * A load balancer implementation randomly choosing an instance.
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random;

    /**
     * Creates a new random load balancer.
     *
     * @param useSecureRandom {@code true} if the load balancer should use a {@link SecureRandom} instance instead of
     *        a {@link Random}
     */
    protected RandomLoadBalancer(boolean useSecureRandom) {
        random = useSecureRandom ? new SecureRandom() : new Random();
    }

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

    @Override
    public boolean requiresStrictRecording() {
        return false;
    }
}
