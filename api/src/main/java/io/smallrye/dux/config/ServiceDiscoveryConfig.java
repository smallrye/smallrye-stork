package io.smallrye.dux.config;

import java.util.Map;

/**
 * Service Discovery configuration
 */
public interface ServiceDiscoveryConfig {

    /**
     * Service discovery type, e.g. "consul".
     * ServiceDiscoveryProvider for the type has to be available
     *
     * @return service discovery type
     */
    String type();

    /**
     * ServiceDiscovery parameters.
     * 
     * @return map of parameters of the service discovery
     */
    Map<String, String> parameters();
}
