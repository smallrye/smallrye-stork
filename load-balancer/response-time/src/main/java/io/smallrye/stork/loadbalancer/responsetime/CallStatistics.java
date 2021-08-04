package io.smallrye.stork.loadbalancer.responsetime;

import io.smallrye.stork.spi.CallStatisticsCollector;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CallStatistics implements CallStatisticsCollector {

    private AtomicLong callCount = new AtomicLong(1);
    private ConcurrentHashMap<Long, CallsData> storage = new ConcurrentHashMap<>();

    static double errorImportanceDeclineFactor = 0.999;
    static double[] declineFactorPowers = new double[16];

    static double responseTimeFactor = 0.5;

    // declineFactorPowers[i] = declineFactor^2^i
    // then for n = \sum_{i=0}^{k} 2 ^ f(i) where f(i) \in {0,1},
    // rescaled error rate is \prod_{i=0}^{k}[f(i) == 1]declineFactorPowers[i]

    static {
        declineFactorPowers[1] = errorImportanceDeclineFactor;
        for (int i = 2; i < 16; i++) {
            declineFactorPowers[i] = declineFactorPowers[i - 1] * declineFactorPowers[i - 1];
        }
    }

    @Override
    public void storeResult(long id, long timeInNs, Exception error) {
        long callIdx = callCount.incrementAndGet();

        while (true) {
            CallsData oldData = storage.get(id);
            if (oldData != null) {
                CallsData newData;
                if (error == null) {
                    double newTotalTime = oldData.weightedTotalTime * responseTimeFactor + timeInNs;
                    double newWeightSum = oldData.weightSum * responseTimeFactor + 1;

                    newData = new CallsData(callIdx, oldData.lastFailure, newTotalTime, newWeightSum,
                            oldData.weightedErrorCount);
                } else {
                    double rescaledFailureRate = oldData.scaledErrorCount(callIdx - oldData.lastFailure);
                    newData = new CallsData(oldData.lastSuccess, callIdx, oldData.weightedTotalTime, oldData.weightSum,
                            rescaledFailureRate + 1);
                }
                if (storage.replace(id, oldData, newData)) {
                    break; // otherwise, try once again, until succeess
                }
            } else {
                // no previously storage data
                CallsData newData;
                if (error == null) {
                    newData = new CallsData(callIdx, 0, timeInNs, 1, 0);
                } else {
                    newData = new CallsData(0, callIdx, 0, 0, 1);
                }

                if (storage.put(callIdx, newData) == null) {
                    break; // success if there was no data (inserted in the meantime)
                }
            }
        }
    }

    public CallsData statsForInstance(long id) {
        return storage.get(id);
    }

    public long currentCall() {
        return callCount.get();
    }

    public CallsData init(long id) {
        CallsData result = new CallsData(0, 0, 0, 0, 0);
        storage.put(id, result);
        return result;
    }

    public static class CallsData {
        final long lastFailure;
        final long lastSuccess;
        final double weightedTotalTime;
        final double weightSum;
        final double weightedErrorCount;
        final AtomicReference<Boolean> forcedAttemptInProgress = new AtomicReference<>(false);

        private CallsData(long lastSuccess, long lastFailure, double weighedTotalTime, double weightSum,
                double weighedErrorCount) {
            this.lastFailure = lastFailure;
            this.lastSuccess = lastSuccess;
            this.weightedTotalTime = weighedTotalTime;
            this.weightSum = weightSum;
            this.weightedErrorCount = weighedErrorCount;
        }

        /**
         * in time, the failures that happened long time (many requests) ago should have decreasing impact.
         * This function decreases the impact of failures that happened long time ago.
         *
         * @param timeSinceLastError amount of requests made, successfully or not, since the last recorded error.
         *        This contains all requests made with the current load balancer, not only the ones made
         *        through this service instance
         * @return new weighed error count
         */
        // TODO : test
        public double scaledErrorCount(long timeSinceLastError) {
            if (weightedErrorCount == 0) {
                return 0;
            }

            // 1 0 1 0 0 0 0 0 is probably better than 0 1 1 1 1 1 1 1 1 1 or even 0 0 1 1 1 1 1 1 1
            // we need something reasonably decreasing with timeSinceLastError increasing.
            // let's do sum_{i=0}^{15} [if_failed(now-i)]0.999^i
            double result = weightedErrorCount;
            for (int i = 1; i < 16; i++) {
                if ((timeSinceLastError & i) == 0) {
                    result *= declineFactorPowers[i];
                }
            }
            return result;
        }

        public double scaledTime() {
            return weightedTotalTime / weightSum;
        }

    }
}
