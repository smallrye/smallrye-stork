package io.smallrye.stork.api;

import io.smallrye.mutiny.Uni;

public interface ServiceRegistrar<MetadataKeyType extends Enum<MetadataKeyType> & MetadataKey> {

    default Uni<Void> registerServiceInstance(String serviceName, String ipAddress, int defaultPort) {
        return registerServiceInstance(serviceName, Metadata.empty(), ipAddress, defaultPort);
    }

    Uni<Void> registerServiceInstance(String serviceName, Metadata<MetadataKeyType> metadata, String ipAddress, int defaultPort);

}
