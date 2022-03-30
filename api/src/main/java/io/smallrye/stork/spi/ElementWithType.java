package io.smallrye.stork.spi;

/**
 * A class having a {@link #type()} method such a load balancer and service discovery providers.
 */
public interface ElementWithType {
    /**
     * Gets the type.
     * 
     * @return the type, must not be {@code null}, must not be blank
     */
    String type();
}
