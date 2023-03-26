package io.smallrye.stork.impl;

import java.util.Optional;

import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.spi.CallStatisticsCollector;

public class ServiceInstanceWithStatGathering implements ServiceInstance {
    private final ServiceInstance delegate;
    private final CallStatisticsCollector statistics;

    volatile long start;

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
    public Optional<String> getPath() {
        return delegate.getPath();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public void recordStart(boolean measureTime) {
        if (measureTime) {
            start = System.nanoTime();
        }
        statistics.recordStart(getId(), measureTime);
    }

    @Override
    public void recordReply() {
        statistics.recordReply(getId(), System.nanoTime() - start);
    }

    @Override
    public void recordEnd(Throwable failure) {
        statistics.recordEnd(getId(), failure);
    }

    /**
     * <b>Do not use for production code</b>
     *
     * mock recording time for tests
     *
     * @param timeInNs time to record, in nanoseconds
     */
    @Deprecated //for tests only
    public void mockRecordingTime(long timeInNs) {
        statistics.recordReply(getId(), timeInNs);
    }
}
