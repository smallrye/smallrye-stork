package io.smallrye.stork.utils;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * A set of utility methods around durations.
 */
public class DurationUtils {

    private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");

    private DurationUtils() {
        // Avoid direct instantiation
    }

    /**
     * Converts a value representing the refresh period which start with a number by implicitly appending `PT` to it.
     * If the value consists only of a number, it implicitly treats the value as seconds.
     * Otherwise, tries to convert the value assuming that it is in the accepted ISO-8601 duration format.
     *
     * @param duration duration as String
     * @return {@link Duration}
     */
    public static Duration parseDuration(String duration) {
        if (duration.startsWith("-")) {
            throw new IllegalArgumentException("Negative refresh-period specified for service discovery: " + duration);
        }
        if (DIGITS.asPredicate().test(duration)) {
            return Duration.ofSeconds(Long.parseLong(duration));
        }
        return Duration.parse("PT" + duration);

    }
}
