package io.smallrye.stork.servicediscovery.kubernetes;

import io.smallrye.stork.api.MetadataKey;

public enum KubernetesMetadataKey implements MetadataKey {

    META_K8S_SERVICE_ID("k8s-service-id");

    private String name;

    KubernetesMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
