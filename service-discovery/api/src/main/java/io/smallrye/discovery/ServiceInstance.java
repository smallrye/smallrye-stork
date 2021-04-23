package io.smallrye.discovery;

public class ServiceInstance {
    // TODO we probably need a service name too
    final long id;

    final String value;

    public ServiceInstance(long id, String value) {
        this.id = id;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
