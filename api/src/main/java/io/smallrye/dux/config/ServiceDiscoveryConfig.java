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
     * @return service discovery rype
     */
    String type();

    /**
     * Service Discovery parameters. May or may not contain a `type` element specifying the type of the service discovery
     * 
     * @return map of parameters of the service discovery
     */
    Map<String, String> parameters();
}
