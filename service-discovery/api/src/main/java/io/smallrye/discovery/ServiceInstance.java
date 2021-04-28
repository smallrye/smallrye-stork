package io.smallrye.discovery;

public class ServiceInstance {
    private final String serviceName;

    private final long id;

    private final String value;

    public ServiceInstance(String serviceName, long id, String value) {
        this.serviceName = serviceName;
        this.id = id;
        this.value = value;
    }

    public String getServiceName() {
        return serviceName;
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
