package io.smallrye.stork;

public class DefaultServiceInstance implements ServiceInstance {

    private final long id;

    private final String host;

    private final int port;

    private final boolean secure;

    public DefaultServiceInstance(long id, String host, int port, boolean secure) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.secure = secure;
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

    public boolean isSecure() {
        return secure;
    }
}
