package io.smallrye.stork.loadbalancer.leastresponsetime.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.loadbalancer.leastresponsetime.impl.util.FastPower;

public class CallStatisticsTest {
    @Test
    void shouldRecordReplyForServiceInstanceWithoutPriorInit() {
        FastPower fastPower = new FastPower(0.9);
        CallStatistics callStatistics = new CallStatistics(60L, fastPower);
        long serviceInstanceId = 821L;

        // without calling init first, this triggers the else branch in storeTime
        callStatistics.recordReply(serviceInstanceId, 100L);

        // verify data is stored correctly
        CallStatistics.CallsData result = callStatistics.statsForInstance(serviceInstanceId);
        assertThat(result).isNotNull();
        assertThat(result.lastRecorded).isEqualTo(2L);
        assertThat(result.weightedTotalTime).isEqualTo(100L);
        assertThat(result.weightSum).isEqualTo(1.0);
    }

    @Test
    void shouldRecordErrorForServiceInstanceWithoutPriorInit() {
        FastPower fastPower = new FastPower(0.9);
        CallStatistics callStatistics = new CallStatistics(60L, fastPower);
        long serviceInstanceId = 821L;

        callStatistics.recordEnd(serviceInstanceId, new RuntimeException("test error"));

        CallStatistics.CallsData result = callStatistics.statsForInstance(serviceInstanceId);
        assertThat(result).isNotNull();
        assertThat(result.lastRecorded).isEqualTo(1L);
        assertThat(result.weightedTotalTime).isEqualTo(60L);
        assertThat(result.weightSum).isEqualTo(1.0);
    }

    @Test
    void shouldHandleMultipleInstancesWithoutPriorInit() {
        FastPower fastPower = new FastPower(0.9);
        CallStatistics callStatistics = new CallStatistics(60L, fastPower);
        long instance1 = 100L;
        long instance2 = 200L;

        callStatistics.recordReply(instance1, 1000L);
        callStatistics.recordReply(instance2, 2000L);

        CallStatistics.CallsData result1 = callStatistics.statsForInstance(instance1);
        CallStatistics.CallsData result2 = callStatistics.statsForInstance(instance2);

        assertThat(result1).isNotNull();
        assertThat(result1.lastRecorded).isEqualTo(2L);
        assertThat(result1.weightedTotalTime).isEqualTo(1000L);

        assertThat(result2).isNotNull();
        assertThat(result2.lastRecorded).isEqualTo(3L);
        assertThat(result2.weightedTotalTime).isEqualTo(2000L);
    }

    @Test
    void shouldMixInitAndNonInitUsage() {
        FastPower fastPower = new FastPower(0.9);
        CallStatistics callStatistics = new CallStatistics(60L, fastPower);

        long initializedInstance = 100L;
        long nonInitializedInstance = 200L;

        callStatistics.init(initializedInstance);
        callStatistics.recordReply(initializedInstance, 1000L);
        callStatistics.recordReply(nonInitializedInstance, 2000L);

        CallStatistics.CallsData result1 = callStatistics.statsForInstance(initializedInstance);
        CallStatistics.CallsData result2 = callStatistics.statsForInstance(nonInitializedInstance);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.weightedTotalTime).isEqualTo(1000L);
        assertThat(result2.weightedTotalTime).isEqualTo(2000L);
    }
}
