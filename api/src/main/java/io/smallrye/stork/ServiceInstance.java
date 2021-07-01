package io.smallrye.stork;

public final class ServiceInstance {

    private final Long id;

    private final String value;

    public ServiceInstance(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
