package io.smallrye.stork.integration;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.smallrye.stork.spi.StorkInfrastructure;

/**
 * An implementation of {@link StorkInfrastructure} that creates utility objects from {@code defaultSupplier}
 * and caches them
 */
public class DefaultStorkInfrastructure implements StorkInfrastructure {
    private final Map<Class<?>, Object> utilities = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> utilityClass, Supplier<T> defaultSupplier) {
        Objects.requireNonNull(utilityClass, "utilityClass cannot be null");
        Objects.requireNonNull(defaultSupplier, "defaultSupplier cannot be null");
        return (T) utilities.computeIfAbsent(utilityClass, key -> defaultSupplier.get());
    }
}
