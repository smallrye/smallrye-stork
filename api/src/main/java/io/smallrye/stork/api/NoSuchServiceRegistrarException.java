package io.smallrye.stork.api;

/**
 * Exception thrown when Stork does not have a {@link ServiceRegistrar} associated with a given name.
 */
public class NoSuchServiceRegistrarException extends RuntimeException {

    /**
     * Creates a new instance of NoSuchServiceRegistrarException.
     *
     * @param registrarName the registrar name
     */
    public NoSuchServiceRegistrarException(String registrarName) {
        super("No service registrar for name " + registrarName);
    }
}
