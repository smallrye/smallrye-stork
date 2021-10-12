package io.smallrye.stork.loadbalancer.leastresponsetime;

import java.util.Collection;

import io.smallrye.stork.LoadBalancer;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.ServiceInstanceWithStatGathering;

public class LeastResponseTimeLoadBalancer implements LoadBalancer {

    // TODO make them configurable
    // TODO sampling instead of collecting everything
    private static final int RETRY_AFTER_FAILURE_THRESHOLD = 10000;
    private static final long FORCE_RETRY_THRESHOLD = 1000;

    private final CallStatistics callStatistics = new CallStatistics();

    // TODO good tests
    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        // we may want sampling in the future. Right now let's collect all the results.
        // compared to IO ops, it should be cheap...
        ServiceInstance best = null;
        CallStatistics.CallsData bestData = null;
        for (ServiceInstance instance : serviceInstances) {
            CallStatistics.CallsData callsData = callStatistics.statsForInstance(instance.getId());
            if (callsData == null) {
                callsData = callStatistics.init(instance.getId()); // to mark that it was used
                callsData.forcedAttemptInProgress.set(true);
                best = instance;
                break;
            } else if (bestData == null) {
                bestData = callsData;
                best = instance;
            } else {
                if (timeToRetry(callsData)) {
                    best = instance;
                    break;
                }
                if (isBetterThan(callsData, bestData)) {
                    best = instance;
                    bestData = callsData;
                }
            }
        }
        return new ServiceInstanceWithStatGathering(best, callStatistics);
    }

    private boolean isBetterThan(CallStatistics.CallsData callsData, CallStatistics.CallsData bestData) {
        if (bestData.lastSuccess != 0) {
            if (callsData.lastSuccess == 0) {
                return false;
            }
            long now = callStatistics.currentCall();
            // TODO we only take into account the best, we should probably use one of the bests
            return callsData.scaledTime() * (1 + callsData.scaledErrorCount(now - bestData.lastFailure)) <= bestData
                    .scaledTime() * (1 + callsData.scaledErrorCount(now - callsData.lastFailure));
        } else if (callsData.lastSuccess != 0) {
            return true;
        } else {
            return callsData.lastFailure == 0 || callsData.lastFailure < bestData.lastFailure;
        }
    }

    private boolean timeToRetry(CallStatistics.CallsData callsData) {
        if (callsData == null) {
            return true;
        }
        long now = callStatistics.currentCall();
        return now - callsData.lastFailure > RETRY_AFTER_FAILURE_THRESHOLD
                && now - callsData.lastSuccess > FORCE_RETRY_THRESHOLD
                && callsData.forcedAttemptInProgress.compareAndSet(false, true);
    }
}
