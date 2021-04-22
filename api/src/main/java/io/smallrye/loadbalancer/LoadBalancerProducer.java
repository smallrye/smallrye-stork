package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

public interface LoadBalancerProducer {
    LoadBalancer getLoadBalancer(TargetAddressProvider addressProvider, Config config);
}
