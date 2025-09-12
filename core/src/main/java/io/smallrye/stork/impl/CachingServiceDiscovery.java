package io.smallrye.stork.impl;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.utils.DurationUtils;

/**
 * Adds the ability to be cached to the ServiceDiscovery implementations.
 *
 * <br>
 * <b>Must be non-blocking</b>
 */
public abstract class CachingServiceDiscovery implements ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(CachingServiceDiscovery.class);
    public static final String REFRESH_PERIOD = "refresh-period";

    public final Duration refreshPeriod;

    public static final String DEFAULT_REFRESH_INTERVAL = "5M";

    private volatile List<ServiceInstance> lastResults;

    private final AtomicReference<Uni<List<ServiceInstance>>> instances = new AtomicReference<>();

    public CachingServiceDiscovery(String refreshPeriod) {
        try {
            this.refreshPeriod = DurationUtils.parseDuration(refreshPeriod, REFRESH_PERIOD);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(REFRESH_PERIOD + " for service discovery should be a number, got: " +
                    refreshPeriod,
                    e);
        }

        this.lastResults = Collections.emptyList();
        Uni<List<ServiceInstance>> retrieval = Uni.createFrom().deferred(() -> fetchNewServiceInstances(this.lastResults)
                .invoke(l -> this.lastResults = l)
                .onFailure().invoke(this::handleFetchError)
                .onFailure().recoverWithItem(this.lastResults));
        this.instances.compareAndSet(null, cache(retrieval));
    }

    /***
     * Configures the period to keep service instances in the cache. Elements will be refetched after the given period.
     * This method can be extended by the provider in order to change the logic for caching service instances.
     *
     * @param uni service instances retrieved from backing discovery source
     * @return cached list of service instances in form of Uni
     */
    public Uni<List<ServiceInstance>> cache(Uni<List<ServiceInstance>> uni) {
        return uni.memoize().atLeast(this.refreshPeriod);
    }

    /**
     * Invalidates the cached service discovery result.
     * <p>
     * This method clears the current memoized {@link Uni} of {@link ServiceInstance} objects
     * by setting it to {@code null}. The next time {@code getInstances()} is called, a new
     * memoized {@code Uni} will be created and the service discovery process will be executed again.
     * <p>
     */
    public void invalidate() {
        this.instances.set(null);
    }

    /**
     *
     * @return all `ServiceInstance`'s for the service
     */
    public Uni<List<ServiceInstance>> getServiceInstances() {
        Uni<List<ServiceInstance>> current = instances.get();
        if (current == null) {
            Uni<List<ServiceInstance>> newInstances = fetchNewServiceInstances(this.lastResults)
                    .invoke(l -> this.lastResults = l)
                    .onFailure().invoke(this::handleFetchError)
                    .onFailure().recoverWithItem(this.lastResults);
            if (instances.compareAndSet(current, newInstances)) {
                instances.set(cache(newInstances));
                return newInstances;
            }
        }
        return instances.get();
    }

    private void handleFetchError(Throwable error) {
        log.error("Failed to fetch service instances", error);
    }

    public abstract Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances);

}
