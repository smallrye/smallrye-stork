package io.smallrye.stork.loadbalancer.leastresponsetime.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.stork.loadbalancer.leastresponsetime.impl.util.FastPower;
import io.smallrye.stork.spi.CallStatisticsCollector;

public class CallStatistics implements CallStatisticsCollector {

    private final AtomicLong callCount = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CallsData> storage = new ConcurrentHashMap<>();

    final long errorPenalty;
    final FastPower powersOfDecliningFactor;

    public CallStatistics(long errorPenalty, FastPower powersOfDecliningFactor) {
        this.errorPenalty = errorPenalty;
        this.powersOfDecliningFactor = powersOfDecliningFactor;
    }

    @Override
    public void storeResult(long id, long timeInNs, Throwable error) {
        long callIdx = callCount.incrementAndGet();
        if (error != null) {
            timeInNs = errorPenalty;
        }
        while (true) {
            CallsData oldData = storage.get(id);

            if (oldData != null) {
                CallsData newData;
                double weightMultiplier = powersOfDecliningFactor.toPower(callIdx - oldData.lastRecorded);
                double newTotalTime = oldData.weightedTotalTime * weightMultiplier + timeInNs;
                double newWeightSum = oldData.weightSum * weightMultiplier + 1;

                newData = new CallsData(callIdx, newTotalTime, newWeightSum);
                if (storage.replace(id, oldData, newData)) {
                    break; // otherwise, try once again, until success
                }
            } else {
                // no previously stored data
                CallsData newData;
                newData = new CallsData(callIdx, timeInNs, 1);

                if (storage.put(callIdx, newData) == null) {
                    break; // success if there was no data (inserted in the meantime)
                }
            }
        }
    }

    // TODO clearing the data for stored service instances if disappeared?
    // TODO or clearing it periodically, etc
    public CallsData statsForInstance(long id) {
        return storage.get(id);
    }

    public long currentCall() {
        return callCount.get();
    }

    public void init(long id) {
        CallsData result = new CallsData(-1, 0, 0);
        storage.put(id, result);
    }

    public static class CallsData {
        public final long lastRecorded;
        public final double weightedTotalTime;
        public final double weightSum;

        private CallsData(long lastRecorded, double weighedTotalTime, double weightSum) {
            this.lastRecorded = lastRecorded;
            this.weightedTotalTime = weighedTotalTime;
            this.weightSum = weightSum;
        }

        public double scaledTime() {
            return weightedTotalTime / weightSum;
        }
    }
}
