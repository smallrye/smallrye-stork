package io.smallrye.stork;

import java.util.Collections;
import java.util.Map;

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
     * @return whether the communication should happen over a secure connection
     */
    boolean isSecure();

    /**
     * @return the metadata of the instance, empty if none.
     */
    default Map<String, Object> getMetadata() {
        return Collections.emptyMap();
    }

    /**
     * @return the labels of the instance, empty if none.
     */
    default Map<String, String> getLabels() {
        return Collections.emptyMap();
    }

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
