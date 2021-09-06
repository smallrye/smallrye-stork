package io.smallrye.stork;

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

    public void recordResult(long timeInNs, Throwable error) {
        statistics.storeResult(getId(), timeInNs, error);
    }
}
