package io.smallrye.stork.api;

import io.smallrye.mutiny.Uni;

public interface ServiceRegistrar<MetadataKeyType extends Enum<MetadataKeyType> & MetadataKey> {

    default Uni<Void> registerServiceInstance(String serviceName, String ipAddress, int defaultPort) {
        checkAddressNotNull(ipAddress);
        return registerServiceInstance(serviceName, Metadata.empty(), ipAddress, defaultPort);
    }

    default void checkAddressNotNull(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || ipAddress.isBlank()) {
            throw new IllegalArgumentException("Parameter ipAddress should be provided.");
        }
    }

    Uni<Void> registerServiceInstance(String serviceName, Metadata<MetadataKeyType> metadata, String ipAddress,
            int defaultPort);

}
