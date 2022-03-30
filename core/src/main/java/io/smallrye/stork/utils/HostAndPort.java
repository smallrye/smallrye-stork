package io.smallrye.stork.utils;

/**
 * Structure representing a host:port address.
 */
public class HostAndPort {
    /**
     * The host.
     */
    public final String host;
    /**
     * The port.
     */
    public final int port;

    /**
     * Creates a new HostAndPort
     *
     * @param host the host
     * @param port the port
     */
    public HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
