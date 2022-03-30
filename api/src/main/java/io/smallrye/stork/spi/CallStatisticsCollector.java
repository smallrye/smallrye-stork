package io.smallrye.stork.spi;

/**
 * If you use {@code ServiceInstanceWithStatGathering}, use an implementation of this class interface
 * for actually collecting information about calls made
 */
public interface CallStatisticsCollector {

    /**
     * Records the start of an operation.
     *
     * @param serviceInstanceId the service instance id
     * @param measureTime whether the time must be measured
     */
    default void recordStart(long serviceInstanceId, boolean measureTime) {
    }

    /**
     * Records the response from an operation.
     *
     * @param serviceInstanceId the service instance id
     * @param timeInNanos the duration of the operation in nanoseconds
     */
    default void recordReply(long serviceInstanceId, long timeInNanos) {

    }

    /**
     * Records the completion of an operation.
     *
     * @param serviceInstanceId the service instance id
     * @param error the error thrown by the operation if the operation failed
     */
    default void recordEnd(long serviceInstanceId, Throwable error) {
    }
}
