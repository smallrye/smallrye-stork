package io.smallrye.stork.utils;

import java.util.Optional;

/**
 * Structure representing a host:port address.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
     * The path, if any.
     */
    public final Optional<String> path;

    /**
     * Creates a new HostAndPort
     *
     * @param host the host
     * @param port the port
     * @param path the path, can be {@code null}
     */
    public HostAndPort(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = Optional.ofNullable(path);
    }

    /**
     * Creates a new HostAndPort
     *
     * @param host the host
     * @param port the port
     */
    public HostAndPort(String host, int port) {
        this(host, port, null);
    }
}
