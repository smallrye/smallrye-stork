package io.smallrye.stork.spi;

import java.util.function.Supplier;

import io.smallrye.stork.api.observability.NoopObservationCollector;
import io.smallrye.stork.api.observability.ObservationCollector;

/**
 * A provider for "utility" objects used by service discovery and load balancer implementations.
 *
 * The default implementation, {@code DefaultStorkInfrastructure} provides objects created by the passed supplier.
 * Vendors can implement their own version of this class to provide custom objects.
 *
 * E.g. Quarkus uses a single Vert.x instance throughout the project and overrides this to return this Vert.x instance
 */
public interface StorkInfrastructure {
    /**
     * Get an instance of a "utility" class
     *
     * @param utilityClass class of the requested object
     * @param defaultSupplier should be used by the implementation to create an object if the environment doesn't provide one,
     *        the result value can be cached.
     * @param <T> type of the utility object
     *
     * @return the utility object
     *
     * @throws NullPointerException if utilityClass or defaultSupplier are null
     */
    <T> T get(Class<T> utilityClass, Supplier<T> defaultSupplier);

    default ObservationCollector getObservationCollector() {
        return new NoopObservationCollector();
    }
}
