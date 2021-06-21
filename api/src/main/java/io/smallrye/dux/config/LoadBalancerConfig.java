package io.smallrye.dux.config;

import java.util.Map;

/**
 * Load balancer configuration
 */
public interface LoadBalancerConfig {

    /**
     * Load balancer type, e.g. "round-robin".
     * A LoadBalancerProvider for the type has to be available
     *
     * @return load balancer type
     */
    String type();

    /**
     * Load Balancer parameters. May or may not contain a `type` element specifying the type of the load balancer
     * 
     * @return map of parameters of the load balancer
     */
    Map<String, String> parameters();
}
