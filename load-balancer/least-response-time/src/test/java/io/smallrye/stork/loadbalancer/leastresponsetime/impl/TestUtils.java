package io.smallrye.stork.loadbalancer.leastresponsetime.impl;

public class TestUtils {
    @SuppressWarnings("deprecation")
    public static void clear(CallStatistics statistics) {
        statistics.clear();
    }
}
