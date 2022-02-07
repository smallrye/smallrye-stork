package io.smallrye.stork.loadbalancer.leastresponsetime;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.ServiceInstanceWithStatGathering;
import io.smallrye.stork.loadbalancer.leastresponsetime.impl.CallStatistics;
import io.smallrye.stork.loadbalancer.leastresponsetime.impl.util.FastPower;
import io.smallrye.stork.utils.DurationUtils;

public class LeastResponseTimeLoadBalancer implements LoadBalancer {

    // TODO sampling instead of collecting everything

    private final CallStatistics callStatistics;
    private final Random random = new SecureRandom();
    private final FastPower powersOfDecliningFactor;

    public LeastResponseTimeLoadBalancer(LeastResponseTimeLoadBalancerProviderConfiguration config) {
        long errorPenalty = DurationUtils.parseDuration(config.getErrorPenalty()).toMillis();
        double decliningFactor = Double.parseDouble(config.getDecliningFactor());
        powersOfDecliningFactor = new FastPower(decliningFactor);
        callStatistics = new CallStatistics(errorPenalty, powersOfDecliningFactor);
    }

    @Override
    public ServiceInstance selectServiceInstance(Collection<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            throw new NoServiceInstanceFoundException("No service instance found");
        }
        // we may want sampling in the future. Right now let's collect all the results.
        // compared to IO ops, it should be cheap...
        ServiceInstance best = null;
        double bestScore = Double.MAX_VALUE;

        for (ServiceInstance instance : serviceInstances) {
            CallStatistics.CallsData callsData = callStatistics.statsForInstance(instance.getId());
            if (callsData == null) {
                callStatistics.init(instance.getId()); // to mark that it was used
                best = instance;
                break;
            } else if (callsData.lastRecorded > -1) {
                // with -1 the initial call is started but not recorded yet
                // ignore such instances for now, choose random from them if no other instances available
                double score = score(callsData);
                if (score < bestScore) {
                    best = instance;
                    bestScore = score;
                }
            }
        }
        if (best == null) {
            int selectedIdx = random.nextInt(serviceInstances.size());
            // it will happen rarely, doing it this way shouldn't hurt us much:
            best = (serviceInstances instanceof List)
                    ? ((List<ServiceInstance>) serviceInstances).get(selectedIdx)
                    : new ArrayList<>(serviceInstances).get(selectedIdx);
        }
        return new ServiceInstanceWithStatGathering(best, callStatistics);
    }

    private double score(CallStatistics.CallsData callsData) {
        return callsData.scaledTime() * powersOfDecliningFactor.toPower(callStatistics.currentCall() - callsData.lastRecorded);
    }
}
