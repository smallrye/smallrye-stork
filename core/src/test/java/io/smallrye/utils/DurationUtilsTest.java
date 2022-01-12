package io.smallrye.utils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.utils.DurationUtils;

public class DurationUtilsTest {

    @Test
    public void testOnlyNumberValueProvided() {
        Duration expectedDuration = Duration.ofSeconds(3);
        Duration actualDuration = DurationUtils.parseDuration("3");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testNumberWithUnitValueProvided() {
        Duration expectedDuration = Duration.ofMinutes(3);
        Duration actualDuration = DurationUtils.parseDuration("3M");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueStartingWithNumberAndInCorrectFormatProvided() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    DurationUtils.parseDuration("-5");
                }).withMessage("Negative refresh-period specified for service discovery: -5");

    }
}
