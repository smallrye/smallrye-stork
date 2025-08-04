package io.smallrye.stork.api;

/**
 * Exception thrown when Stork does not have a {@link Service} associated with a given name.
 */
public class NoSuchServiceDefinitionException extends RuntimeException {

    /**
     * Creates a new instance of NoSuchServiceDefinitionException.
     *
     * @param serviceName the service name
     */
    public NoSuchServiceDefinitionException(String serviceName) {
        super("No service configuration defined for name " + serviceName);
    }
}
