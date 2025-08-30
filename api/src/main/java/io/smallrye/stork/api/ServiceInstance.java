package io.smallrye.stork.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
     * For service behind an API gateway or a proxy, this method return the path.
     * When set, the final location of the service is composed of {@code $host:$port/$path}.
     *
     * @return the path if any.
     */
    Optional<String> getPath();

    /**
     * @return whether the communication should happen over a secure connection
     */
    boolean isSecure();

    /**
     * @return the metadata of the instance, empty if none.
     */
    default Metadata<? extends MetadataKey> getMetadata() {
        return Metadata.empty();
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
     * <b>Warning</b> Usually should not be called directly.
     * Most client libraries should use {@link Service#selectInstanceFromListAndRecordStart(Collection, boolean)} and
     * {@link Service#selectInstanceAndRecordStart(boolean)}
     * to select services. These methods invoke this method automatically
     *
     * <br>
     * When {@code gatherStatistics} is enabled, reports the start of an operation using this service instance.
     * <p>
     * The load balancers that keep track of inflight operations should increase the counter on this method.
     * The load balancer that collect times of operations should only take into account operations that have
     * {@code measureTime} set to {@code true}
     *
     * @param measureTime if true, {@link #recordReply()} will be called for this operation
     */
    default void recordStart(boolean measureTime) {
    }

    /**
     * When {@code gatherStatistics} is enabled, reports a reply for an operation using this service instance.
     * <p>
     * Should be called if and only if {@code recordStart(true)} has been called earlier
     */
    default void recordReply() {

    }

    /**
     * When {@code gatherStatistics} is enabled, reports the end of an operation using this service instance.
     *
     * @param failure if the operation failed, the throwable depicting the failure, {@code null} otherwise
     */
    default void recordEnd(Throwable failure) {
    }
}
