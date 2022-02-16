package io.smallrye.stork.spi;

/**
 * If you use {@code ServiceInstanceWithStatGathering}, use an implementation of this class interface
 * for actually collecting information about calls made
 */
public interface CallStatisticsCollector {

    default void recordStart(long serviceInstanceId, boolean measureTime) {
    }

    default void recordReply(long serviceInstanceId, long timeInNanos) {

    }

    default void recordEnd(long serviceInstanceId, Throwable error) {
    }
}
