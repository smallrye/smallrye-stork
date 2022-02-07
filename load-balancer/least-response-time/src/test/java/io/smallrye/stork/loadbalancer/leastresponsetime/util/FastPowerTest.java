package io.smallrye.stork.loadbalancer.leastresponsetime.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.loadbalancer.leastresponsetime.impl.util.FastPower;

public class FastPowerTest {

    @Test
    void shouldWorkOnPowersOfTwo() {
        FastPower power = new FastPower(2);
        assertThat(power.toPower(0L)).isCloseTo(1., Percentage.withPercentage(0.1));
        assertThat(power.toPower(1L)).isCloseTo(2., Percentage.withPercentage(0.1));
        assertThat(power.toPower(10L)).isCloseTo(1024., Percentage.withPercentage(0.1));
        assertThat(power.toPower(30L)).isCloseTo(1073741824, Percentage.withPercentage(0.1));
    }

    @Test
    void shouldWorkOnPowersOfOneTenth() {
        FastPower power = new FastPower(0.1);
        assertThat(power.toPower(0L)).isCloseTo(1., Percentage.withPercentage(0.1));
        assertThat(power.toPower(1L)).isCloseTo(0.1, Percentage.withPercentage(0.1));
        assertThat(power.toPower(2L)).isCloseTo(0.01, Percentage.withPercentage(0.1));
        assertThat(power.toPower(10L)).isCloseTo(0.0000000001, Percentage.withPercentage(0.1));
        assertThat(power.toPower(30L)).isCloseTo(0.000000000000000000000000000001, Percentage.withPercentage(0.1));
    }
}
