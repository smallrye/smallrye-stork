package io.smallrye.stork.api.observability;

public interface EventCompletionHandler {
    void complete(StorkResolutionEvent event);
}
