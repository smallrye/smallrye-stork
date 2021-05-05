package io.smallrye.discovery;

public class ServiceInstance {
    private final long id;

    private final String value;

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
