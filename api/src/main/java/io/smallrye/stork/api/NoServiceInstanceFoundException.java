package io.smallrye.stork.api;

/**
 * Thrown by a {@link LoadBalancer} when it doesn't have service instances to choose from
 * or all available services are not valid to select, e.g. are determined to be faulty
 */
public class NoServiceInstanceFoundException extends RuntimeException {

    /**
     * Creates a new NoServiceInstanceFoundException.
     * 
     * @param message the error message
     */
    public NoServiceInstanceFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new NoServiceInstanceFoundException.
     *
     * @param message the error message
     * @param cause the cause
     */
    public NoServiceInstanceFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
