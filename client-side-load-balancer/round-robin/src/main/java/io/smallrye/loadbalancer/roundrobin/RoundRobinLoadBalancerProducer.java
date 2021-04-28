package io.smallrye.loadbalancer.roundrobin;

import io.smallrye.discovery.ServiceDiscoveryProducer;
import io.smallrye.loadbalancer.LoadBalancer;
import io.smallrye.loadbalancer.LoadBalancerProducer;

public class RoundRobinLoadBalancerProducer implements LoadBalancerProducer {

    private final ServiceDiscoveryProducer serviceDiscoveryProducer;

    public RoundRobinLoadBalancerProducer(ServiceDiscoveryProducer serviceDiscoveryProducer) {
        this.serviceDiscoveryProducer = serviceDiscoveryProducer;
    }

    @Override
    public LoadBalancer getLoadBalancer(String serviceName) {
        return new RoundRobinLoadBalancer(serviceDiscoveryProducer.getServiceDiscovery(serviceName));
    }
}
