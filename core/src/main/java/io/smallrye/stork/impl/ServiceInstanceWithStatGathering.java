package io.smallrye.stork.impl;

import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.spi.CallStatisticsCollector;

public class ServiceInstanceWithStatGathering implements ServiceInstance {
    private final ServiceInstance delegate;
    private final CallStatisticsCollector statistics;

    public ServiceInstanceWithStatGathering(ServiceInstance delegate, CallStatisticsCollector statistics) {
        this.delegate = delegate;
        this.statistics = statistics;
    }

    @Override
    public boolean gatherStatistics() {
        return true;
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @Override
    public String getHost() {
        return delegate.getHost();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    public void recordResult(long timeInNs, Throwable failure) {
        statistics.storeResult(getId(), timeInNs, failure);
    }
}
