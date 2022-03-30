package io.smallrye.stork.servicediscovery.consul;

import io.smallrye.stork.api.MetadataKey;

/**
 * The Consul metadata keys.
 */
public enum ConsulMetadataKey implements MetadataKey {

    /**
     * The key for the consul service id.
     */
    META_CONSUL_SERVICE_ID("consul-service-id"),
    /**
     * The key for the consul service node.
     */
    META_CONSUL_SERVICE_NODE("consul-service-node"),
    /**
     * The key for the consul service node address.
     */
    META_CONSUL_SERVICE_NODE_ADDRESS("consul-service-node-address");

    private final String name;

    /**
     * Creates a new ConsulMetadataKey
     * 
     * @param name the name
     */
    ConsulMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
