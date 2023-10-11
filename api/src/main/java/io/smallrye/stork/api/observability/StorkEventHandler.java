package io.smallrye.stork.api.observability;

public interface StorkEventHandler {
    void complete(StorkObservation event);
}
