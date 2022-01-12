package io.smallrye.stork.api.config;

import java.util.Map;

/**
 * Service Discovery configuration. Only used internally. The configuration is translated to an object
 * that provides parameters through getters
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
