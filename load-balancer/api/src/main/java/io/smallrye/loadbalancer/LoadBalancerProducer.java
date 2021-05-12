package io.smallrye.loadbalancer;

public interface LoadBalancerProducer {
    LoadBalancer getLoadBalancer(String serviceName);
}
