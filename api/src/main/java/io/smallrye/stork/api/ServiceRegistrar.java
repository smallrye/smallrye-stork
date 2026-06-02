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

    default void checkInstanceNameNotNull(String instanceName) {
        if (instanceName == null || instanceName.isEmpty() || instanceName.isBlank()) {
            throw new IllegalArgumentException("Parameter instanceName should be provided.");
        }
    }

    default void checkRegistrarOptionsNotNull(RegistrarOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Parameter registrar options should be provided.");
        }
    }

    default Uni<Void> registerServiceInstance(String serviceName, Metadata<MetadataKeyType> metadata, String ipAddress,
            int defaultPort) {
        return registerServiceInstance(serviceName, null, metadata, ipAddress, defaultPort);
    }

    Uni<Void> registerServiceInstance(String serviceName, String instanceName, Metadata<MetadataKeyType> metadata,
            String ipAddress, int defaultPort);

    Uni<Void> registerServiceInstance(String serviceName, String instanceName, List<String> tags,
            Metadata<MetadataKeyType> metadata, String ipAddress, int defaultPort);

    default Uni<Void> registerServiceInstance(RegistrarOptions options) {
        checkRegistrarOptionsNotNull(options);
        checkAddressNotNull(options.ipAddress());

        return registerServiceInstance(options.serviceName(), options.ipAddress(), options.defaultPort());
    }

    @Deprecated
    Uni<Void> deregisterServiceInstance(String serviceName);

    /**
     * Deregisters a specific service instance identified by instance name.
     * Implementors SHOULD override this method.
     * The default implementation falls back to {@link #deregisterServiceInstance(String)},
     * which may deregister ALL instances of the service.
     */
    default Uni<Void> deregisterServiceInstance(String serviceName, String instanceName) {
        return deregisterServiceInstance(serviceName);
    }

    /**
     * Deregisters a specific service instance identified by its IP address and port.
     * Implementors SHOULD override this method.
     * The default implementation falls back to {@link #deregisterServiceInstance(String)},
     * which may deregister ALL instances of the service.
     */
    default Uni<Void> deregisterServiceInstance(String serviceName, String ipAddress, int port) {
        return deregisterServiceInstance(serviceName);
    }

    record RegistrarOptions(String serviceName, String ipAddress, int defaultPort, List<String> tags,
            Map<String, String> metadata) {
    }

}
