package io.smallrye.stork;

import java.util.Collections;
import java.util.Map;

public class DefaultServiceInstance implements ServiceInstance {

    private final long id;

    private final String host;

    private final int port;

    private final boolean secure;

    private final Map<String, String> labels;

    private final Map<String, Object> metadata;

    public DefaultServiceInstance(long id, String host, int port, boolean secure) {
        this(id, host, port, secure, Collections.emptyMap(), Collections.emptyMap());
    }

    public DefaultServiceInstance(long id, String host, int port, boolean secure, Map<String, String> labels,
            Map<String, Object> metadata) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.labels = Collections.unmodifiableMap(labels);
        this.metadata = Collections.unmodifiableMap(metadata);
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
    public boolean isSecure() {
        return secure;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }
}
