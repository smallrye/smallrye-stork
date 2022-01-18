package io.smallrye.stork.spi;

/**
 * If you use {@code ServiceInstanceWithStatGathering}, use an implementation of this class interface
 * for actually collecting information about calls made
 */
public interface CallStatisticsCollector {
    /**
     * invoked by {@code ServiceInstanceWithStatGathering} when a call is finished
     * 
     * @param id call identifier
     * @param time time that a call took
     * @param error if the call failed, the error it failed with
     */
    void storeResult(long id, long time, Throwable error);
}
