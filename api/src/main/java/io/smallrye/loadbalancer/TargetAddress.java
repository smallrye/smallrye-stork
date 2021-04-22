package io.smallrye.loadbalancer;

import java.net.URI;

public class TargetAddress {
    final URI value; // TODO that may be not the best of types, maybe String and some converters?
    final long id;

    public TargetAddress(URI value, long id) {
        this.value = value;
        this.id = id;
    }

    public URI getValue() {
        return value;
    }

    public long getId() {
        return id;
    }
}
