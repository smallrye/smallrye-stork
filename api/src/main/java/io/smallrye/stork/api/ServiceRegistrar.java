package io.smallrye.stork.api;

import java.util.List;
import java.util.Map;

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

    default void checkRegistrarOptionsNotNull(RegistrarOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Parameter registrar options should be provided.");
        }
    }

    Uni<Void> registerServiceInstance(String serviceName, Metadata<MetadataKeyType> metadata, String ipAddress,
            int defaultPort);

    default Uni<Void> registerServiceInstance(RegistrarOptions options) {
        checkRegistrarOptionsNotNull(options);
        checkAddressNotNull(options.ipAddress());

        return registerServiceInstance(options.serviceName(), options.ipAddress(), options.defaultPort());
    }

    Uni<Void> deregisterServiceInstance(String serviceName);

    record RegistrarOptions(String serviceName, String ipAddress, int defaultPort, List<String> tags,
            Map<String, String> metadata) {
    }

}
