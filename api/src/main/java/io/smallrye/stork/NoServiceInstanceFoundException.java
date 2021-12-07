package io.smallrye.stork;

/**
 * Thrown by a {@link LoadBalancer} when it doesn't have service instances to choose from
 * or all available services are not valid to select, e.g. are determined to be faulty
 */
public class NoServiceInstanceFoundException extends RuntimeException {
    public NoServiceInstanceFoundException(String message) {
        super(message);
    }

    public NoServiceInstanceFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
