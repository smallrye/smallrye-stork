package io.smallrye.stork.impl;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

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

    private final Uni<List<ServiceInstance>> instances;

    public CachingServiceDiscovery(String refreshPeriod) {
        try {
            // TODO: document it
            this.refreshPeriod = DurationUtils.parseDuration(refreshPeriod, REFRESH_PERIOD);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(REFRESH_PERIOD + " for service discovery should be a number, got: " +
                    refreshPeriod,
                    e);
        }

        this.lastResults = Collections.emptyList();
        this.instances = Uni.createFrom().deferred(() -> fetchNewServiceInstances(this.lastResults)
                .invoke(l -> this.lastResults = l)
                .onFailure().invoke(this::handleFetchError)
                .onFailure().recoverWithItem(this.lastResults))
                .memoize().atLeast(this.refreshPeriod);
    }

    /**
     *
     * @return all `ServiceInstance`'s for the service
     */
    public Uni<List<ServiceInstance>> getServiceInstances() {
        return instances;
    }

    private void handleFetchError(Throwable error) {
        log.error("Failed to fetch service instances", error);
    }

    public abstract Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances);
}
