package io.smallrye.stork.servicediscovery.kubernetes;

import io.smallrye.stork.api.MetadataKey;

/**
 * Kubernetes metadata keys.
 */
public enum KubernetesMetadataKey implements MetadataKey {

    /**
     * The key to access the kubernetes service id
     */
    META_K8S_SERVICE_ID("k8s-service-id"),
    /**
     * The key to access the kubernates namespace
     */
    META_K8S_NAMESPACE("k8s-namespace"),

    /**
     * The key to access the port protocol
     */
    META_K8S_PORT_PROTOCOL("k8s-port-protocol");

    private final String name;

    KubernetesMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
