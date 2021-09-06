package io.smallrye.stork.spi;

public interface CallStatisticsCollector {
    void storeResult(long id, long time, Throwable error);
}
