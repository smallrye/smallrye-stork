package io.smallrye.stork.api;

/**
 * Exception thrown when Stork does not have a {@link Service} associated with a given name.
 */
public class NoSuchServiceDefinitionException extends RuntimeException {

    public NoSuchServiceDefinitionException(String serviceName) {
        super("No service defined for name " + serviceName);
    }
}
