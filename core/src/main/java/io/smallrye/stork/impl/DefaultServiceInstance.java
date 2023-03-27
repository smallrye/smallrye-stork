package io.smallrye.stork.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.MetadataKey;
import io.smallrye.stork.api.ServiceInstance;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DefaultServiceInstance implements ServiceInstance {

    private final long id;

    private final String host;

    private final int port;

    private final boolean secure;

    private final Optional<String> path;

    private final Map<String, String> labels;

    private final Metadata<? extends MetadataKey> metadata;

    public DefaultServiceInstance(long id, String host, int port, String path, boolean secure) {
        this(id, host, port, Optional.ofNullable(path), secure, Collections.emptyMap(),
                Metadata.empty());
    }

    public DefaultServiceInstance(long id, String host, int port, Optional<String> path, boolean secure) {
        this(id, host, port, path, secure, Collections.emptyMap(),
                Metadata.empty());
    }

    public DefaultServiceInstance(long id, String host, int port, boolean secure) {
        this(id, host, port, Optional.empty(), secure, Collections.emptyMap(),
                Metadata.empty());
    }

    public DefaultServiceInstance(long id, String host, int port, boolean secure, Metadata<? extends MetadataKey> metadata) {
        this(id, host, port, Optional.empty(), secure, Collections.emptyMap(),
                metadata);
    }

    public DefaultServiceInstance(long id, String host, int port, boolean secure, Map<String, String> labels,
            Metadata<? extends MetadataKey> metadata) {
        this(id, host, port, Optional.empty(), secure, labels, metadata);
    }

    public DefaultServiceInstance(long id, String host, int port, Optional<String> path, boolean secure,
            Map<String, String> labels,
            Metadata<? extends MetadataKey> metadata) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.path = path;
        this.labels = Collections.unmodifiableMap(labels);
        this.metadata = metadata;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Optional<String> getPath() {
        return path;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public Metadata<? extends MetadataKey> getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }
}
