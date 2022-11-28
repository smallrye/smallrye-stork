package io.smallrye.stork.servicediscovery.knative;

import io.smallrye.stork.api.MetadataKey;

/**
 * Knative metadata keys.
 */
public enum KnativeMetadataKey implements MetadataKey {

    /**
     * The key to access the knative service id
     */
    META_KNATIVE_SERVICE_ID("knative-service-id"),
    /**
     * The key to access the knative namespace
     */
    META_KNATIVE_NAMESPACE("knative-namespace"),

    /**
     * The key to access the knative revision name
     */
    META_KNATIVE_LATEST_REVISION("knative-latest-revision");

    private final String name;

    KnativeMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
