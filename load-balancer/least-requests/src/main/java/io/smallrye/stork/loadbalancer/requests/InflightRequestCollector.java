package io.smallrye.stork.loadbalancer.requests;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.stork.spi.CallStatisticsCollector;

/**
 * {@link CallStatisticsCollector} that keep tracks of the number of inflight requests.
 */
public class InflightRequestCollector implements CallStatisticsCollector {

    private final ConcurrentHashMap<Long, AtomicInteger> storage = new ConcurrentHashMap<>();

    /**
     * Gets the number of inflight requests for the service instance with the given {@code id}.
     *
     * @param id the service instance id
     * @return the number of inflight request, {@code 0} if none.
     */
    public int get(long id) {
        return storage.computeIfAbsent(id, x -> new AtomicInteger(0)).get();
    }

    @Override
    public void recordStart(long serviceInstanceId, boolean measureTime) {
        storage.get(serviceInstanceId).incrementAndGet();
    }

    @Override
    public void recordEnd(long serviceInstanceId, Throwable throwable) {
        storage.get(serviceInstanceId).decrementAndGet();
    }
}
