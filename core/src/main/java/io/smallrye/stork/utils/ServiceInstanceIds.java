package io.smallrye.stork.utils;

import java.util.concurrent.atomic.AtomicLong;

// TODO: is this sufficient?
public class ServiceInstanceIds {

    private static final AtomicLong idSequence = new AtomicLong();

    public static Long next() {
        return idSequence.getAndIncrement();
    }

    private ServiceInstanceIds() {
    }
}
