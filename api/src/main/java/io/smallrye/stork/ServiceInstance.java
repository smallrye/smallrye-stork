package io.smallrye.stork;

/**
 * Represents an instance of service.
 */
public interface ServiceInstance {

    /**
     * @return the service id. The service ids are unique per Stork instance.
     */
    long getId();

    /**
     * @return the host of the service.
     */
    String getHost();

    /**
     * @return the port of the service.
     */
    int getPort();

    /**
     * @return whether the interaction with the service are monitored, allowing statistic-based load-balancing.
     */
    default boolean gatherStatistics() {
        return false;
    }

    /**
     * When {@code gatherStatistics} is enabled, reports the completion of an operation using this service instance.
     *
     * @param timeInNs the duration of the operation in nano-seconds.
     * @param failure the failure if the operation failed. Can be {@code null}.
     */
    default void recordResult(long timeInNs, Throwable failure) {
    }
}
