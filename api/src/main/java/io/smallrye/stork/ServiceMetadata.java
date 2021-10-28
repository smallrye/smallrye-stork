package io.smallrye.stork;

import java.util.Map;

public class ServiceMetadata {

    private final Map<String, String> labels;
    //TODO rename
    private final Map<String, Object> metadata;

    public ServiceMetadata(Map<String, String> labels, Map<String, Object> metadata) {
        this.labels = labels;
        this.metadata = metadata;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
