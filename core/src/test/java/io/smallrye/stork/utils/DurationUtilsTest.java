package io.smallrye.stork.utils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class DurationUtilsTest {

    @Test
    public void testOnlyNumberValueProvided() {
        Duration expectedDuration = Duration.ofSeconds(3);
        Duration actualDuration = DurationUtils.parseDuration("3", "refresh-period");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testNumberWithUnitValueProvided() {
        Duration expectedDuration = Duration.ofMinutes(3);
        Duration actualDuration = DurationUtils.parseDuration("3M", "refresh-period");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueStartingWithNumberAndInCorrectFormatProvided() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    DurationUtils.parseDuration("-5", "refresh-period");
                }).withMessage("Negative refresh-period specified for service discovery: -5");

    }
}
