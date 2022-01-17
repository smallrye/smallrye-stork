package io.smallrye.stork.loadbalancer.leastresponsetime;

import java.util.Collection;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;

public class LeastResponseTimeLoadBalancer implements LoadBalancer {

    // TODO sampling instead of collecting everything

    private final CallStatistics callStatistics = new CallStatistics();
    private final long retryAfterFailureTreshold;
    private final long forceRetryThreshold;

    public LeastResponseTimeLoadBalancer(LeastResponseTimeLoadBalancerProviderConfiguration config) {
        this.retryAfterFailureTreshold = Long.parseLong(config.getRetryAfterFailureThreshold());
        this.forceRetryThreshold = Long.parseLong(config.getForceRetryThreshold());
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }
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
            // @formatter:off
            return callsData.scaledTime() * (1 + callsData.scaledErrorCount(now - callsData.lastFailure))
                    <= bestData.scaledTime() * (1 + bestData.scaledErrorCount(now - bestData.lastFailure));
            // @formatter:on
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
        return now - callsData.lastFailure > retryAfterFailureTreshold
                && now - callsData.lastSuccess > forceRetryThreshold
                && callsData.forcedAttemptInProgress.compareAndSet(false, true);
    }
}
