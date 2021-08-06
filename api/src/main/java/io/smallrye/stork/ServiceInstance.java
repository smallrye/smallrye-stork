package io.smallrye.stork;

public interface ServiceInstance {
    long getId();

    String getHost();

    int getPort();

    default boolean gatherStatistics() {
        return false;
    }

    default void recordResult(long time, Exception error) {
    }
}
