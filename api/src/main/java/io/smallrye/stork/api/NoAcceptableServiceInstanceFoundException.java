package io.smallrye.stork.api;

/**
 * Thrown by {@link LoadBalancer} when all available services are not acceptable for some, arbitrary, reason
 */
public class NoAcceptableServiceInstanceFoundException extends NoServiceInstanceFoundException {

    /**
     * Creates a new NoAcceptableServiceInstanceFoundException.
     *
     * @param message the message
     */
    public NoAcceptableServiceInstanceFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new NoAcceptableServiceInstanceFoundException.
     *
     * @param message the message
     * @param cause the cause
     */
    public NoAcceptableServiceInstanceFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
