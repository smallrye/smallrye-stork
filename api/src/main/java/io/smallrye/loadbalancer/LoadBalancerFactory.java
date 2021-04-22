package io.smallrye.loadbalancer;

public interface LoadBalancerFactory {
    LoadBalancer get(String name);
}
