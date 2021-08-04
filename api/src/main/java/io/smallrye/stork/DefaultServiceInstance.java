package io.smallrye.stork;

public class DefaultServiceInstance implements ServiceInstance {

    private final long id;

    private final String host;

    private final int port;

    public DefaultServiceInstance(long id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
