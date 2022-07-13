package io.smallrye.stork.servicediscovery.eureka;

import io.smallrye.stork.api.MetadataKey;

/**
 * The Eureka metadata keys.
 */
public enum EurekaMetadataKey implements MetadataKey {

    /**
     * The key for the eureka service id.
     */
    META_EUREKA_SERVICE_ID("eureka-service-id");

    private final String name;

    /**
     * Creates a new ConsulMetadataKey
     * 
     * @param name the name
     */
    EurekaMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
