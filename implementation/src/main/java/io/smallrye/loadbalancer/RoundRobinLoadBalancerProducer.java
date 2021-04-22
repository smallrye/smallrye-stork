package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

public class RoundRobinLoadBalancerProducer implements LoadBalancerProducer{
    @Override
    public LoadBalancer getLoadBalancer(TargetAddressProvider addressProvider, Config config) {
        return new RoundRobinLoadBalancer(config, addressProvider);
    }
}
