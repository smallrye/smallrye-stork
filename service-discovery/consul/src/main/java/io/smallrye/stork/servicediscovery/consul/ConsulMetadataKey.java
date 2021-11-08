package io.smallrye.stork.servicediscovery.consul;

import io.smallrye.stork.MetadataKey;

public enum ConsulMetadataKey implements MetadataKey {

    META_CONSUL_SERVICE_ID("consul-service-id"),
    META_CONSUL_SERVICE_NODE("consul-service-node"),
    META_CONSUL_SERVICE_NODE_ADDRESS("consul-service-node-address");

    private String name;

    ConsulMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
