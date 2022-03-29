package io.smallrye.stork.loadbalancer.random;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoAcceptableServiceInstanceFoundException;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;
import io.smallrye.stork.spi.CallStatisticsCollector;

/**
 * Select a single instance and use it until it fails.
 * On failure, store the time of failure in `failedInstances` map.
 * <p/>
 * When new instance selection is needed:
 * <ul>
 * <li>if there's an instance for which we don't have a failure recorded, use it</li>
 * <li>otherwise, pick the instance whose failure was the longest time away and:
 * <ul>
 * <li>return it if {@code failureBackOff} has passed since its last failure</li>
 * <li>throw NoAcceptableServiceInstanceFoundException if it has not</li>
 * </ul>
 * </li>
 * </ul>
 */
public class StickyLoadBalancer implements LoadBalancer, CallStatisticsCollector {

    private final long failureBackOffNs;

    private final LinkedHashMap<Long, Long> failedInstances = new LinkedHashMap<>();
    private volatile ServiceInstanceWithStatGathering lastSelected;

    public StickyLoadBalancer(Duration failureBackOff) {
        this.failureBackOffNs = failureBackOff.toNanos();
    }

    // TODO start randomly, maybe sort the collection by service instance ids
    // TODO: flow chart for how it works alongside the text
    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }

        if (lastSelected != null) {
            for (var instance : serviceInstances) {
                if (instance.getId() == lastSelected.getId()) {
                    return lastSelected;
                }
            }
        }
        Map<Long, ServiceInstance> instanceMap = serviceInstances.stream()
                .collect(Collectors.toMap(ServiceInstance::getId, i -> i));
        lastSelected = selectNextInstance(instanceMap);
        return lastSelected;
    }

    private ServiceInstanceWithStatGathering selectNextInstance(Map<Long, ServiceInstance> serviceInstances) {
        for (ServiceInstance serviceInstance : serviceInstances.values()) {
            if (!failedInstances.containsKey(serviceInstance.getId())) {
                return new ServiceInstanceWithStatGathering(serviceInstance, this);
            }
        }

        Iterator<Map.Entry<Long, Long>> failedInstanceIterator = failedInstances.entrySet().iterator();

        long now = System.nanoTime();
        while (failedInstanceIterator.hasNext()) {
            Map.Entry<Long, Long> oldestFailedInstance = failedInstanceIterator.next();

            Long instanceId = oldestFailedInstance.getKey();
            Long lastFailureTime = oldestFailedInstance.getValue();
            if (now - lastFailureTime < failureBackOffNs) {
                break;
            }
            failedInstanceIterator.remove();
            ServiceInstance serviceInstance = serviceInstances.get(instanceId);
            if (serviceInstance != null) {
                lastSelected = new ServiceInstanceWithStatGathering(serviceInstance, this);
                return lastSelected;
            }
        }
        throw new NoAcceptableServiceInstanceFoundException("Each of the available service instances failed " +
                "within the configured failure-backoff-time");
    }

    @Override
    public void recordEnd(long serviceInstanceId, Throwable error) {
        recordEndAtTime(serviceInstanceId, error, System.nanoTime());
    }

    // exposed for tests only
    void recordEndAtTime(long serviceInstanceId, Throwable error, long currentTime) {
        if (error != null) {
            failedInstances.put(serviceInstanceId, currentTime);
            lastSelected = null;
        }
    }

    @Deprecated // for tests only
    LinkedHashMap<Long, Long> getFailedInstances() {
        return new LinkedHashMap<>(failedInstances);
    }
}
