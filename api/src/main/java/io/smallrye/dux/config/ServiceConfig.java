package io.smallrye.dux.config;

public interface ServiceConfig {
    String serviceName();

    LoadBalancerConfig loadBalancer();

    ServiceDiscoveryConfig serviceDiscovery();

}
