package io.smallrye.stork;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.utils.DurationUtils;

/**
 * Adds the ability to be cached to the ServiceDiscovery implementations.
 *
 * <br>
 * <b>Must be non-blocking</b>
 */
public abstract class CachingServiceDiscovery implements ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(CachingServiceDiscovery.class);

    public final Duration refreshPeriod;
    public static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);

    private volatile List<ServiceInstance> lastResults;

    private final Uni<List<ServiceInstance>> instances;

    public CachingServiceDiscovery(ServiceDiscoveryConfig config) {
        String period = config.parameters().get("refresh-period");
        try {
            // TODO: document it
            this.refreshPeriod = period != null
                    ? DurationUtils.parseDuration(period)
                    : DEFAULT_REFRESH_INTERVAL;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("refresh-period for service discovery should be a number, got: " +
                    period,
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
