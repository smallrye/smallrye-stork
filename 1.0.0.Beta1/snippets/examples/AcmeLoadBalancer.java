package examples;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.LoadBalancerConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class AcmeLoadBalancer implements LoadBalancer {

    private final Random random;

    public AcmeLoadBalancer(LoadBalancerConfig config) {
        random = new Random();
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        int index = random.nextInt(serviceInstances.size());
        return new ArrayList<>(serviceInstances).get(index);
    }
}
