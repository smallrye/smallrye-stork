package io.smallrye.stork;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

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

    private volatile ServiceInstancesCache cacheData;
    public final Duration refreshPeriod;
    public static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);

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
    }

    /**
     *
     * @return all `ServiceInstance`'s for the service
     */
    public Uni<List<ServiceInstance>> getServiceInstances() {
        if (refreshNotNeed()) {
            return Uni.createFrom().item(cacheData.getServiceInstances());
        }
        Uni<List<ServiceInstance>> serviceInstances = fetchNewServiceInstances();
        return serviceInstances.onItem()
                .invoke(services -> this.cacheData = new ServiceInstancesCache(services, LocalDateTime.now()));
    }

    public boolean refreshNotNeed() {
        return cacheData != null
                && cacheData.getLastFetchDateTime().isAfter(LocalDateTime.now().minus(refreshPeriod));
    }

    public abstract Uni<List<ServiceInstance>> fetchNewServiceInstances();
}
