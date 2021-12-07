package io.smallrye.stork;

/**
 * Thrown by {@link LoadBalancer} when all available services are not acceptable for some, arbitrary, reason
 */
public class NoAcceptableServiceInstanceFoundException extends NoServiceInstanceFoundException {
    public NoAcceptableServiceInstanceFoundException(String message) {
        super(message);
    }

    public NoAcceptableServiceInstanceFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
