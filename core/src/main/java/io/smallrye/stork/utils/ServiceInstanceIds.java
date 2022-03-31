package io.smallrye.stork.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service instance id generator.
 */
// TODO: is this sufficient?
public class ServiceInstanceIds {

    private static final AtomicLong idSequence = new AtomicLong();

    /**
     * Gets the next, unused instance id.
     *
     * @return the next instance id
     */
    public static Long next() {
        return idSequence.getAndIncrement();
    }

    private ServiceInstanceIds() {
        // Avoid direct instantiation
    }
}
