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
    public Duration refreshPeriod;
    public static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);

    public CachingServiceDiscovery(ServiceDiscoveryConfig config) {
        String refreshPeriod = config.parameters().get("refresh-period");
        try {
            // TODO: document it
            this.refreshPeriod = refreshPeriod != null
                    ? DurationUtils.parseDuration(refreshPeriod)
                    : DEFAULT_REFRESH_INTERVAL;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("refresh-period for service discovery should be a number, got: " +
                    refreshPeriod,
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
