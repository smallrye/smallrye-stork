package io.smallrye.stork.utils;

import java.time.Duration;
import java.util.regex.Pattern;

public class DurationUtils {

    public static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");

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
            return Duration.ofSeconds(Long.valueOf(duration));
        }
        return Duration.parse("PT" + duration);

    }
}
