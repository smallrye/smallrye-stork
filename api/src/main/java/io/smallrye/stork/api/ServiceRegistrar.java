package io.smallrye.stork.api;

import io.smallrye.mutiny.Uni;

public interface ServiceRegistrar<MetadataKeyType extends Enum<MetadataKeyType> & MetadataKey> {

    default Uni<Void> registerServiceInstance(String serviceName, String ipAddress, int port) {
        return registerServiceInstance(serviceName, Metadata.empty(), ipAddress, port);
    }

    Uni<Void> registerServiceInstance(String serviceName, Metadata<MetadataKeyType> metadata, String ipAddress, int port);
}
