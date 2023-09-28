package io.smallrye.stork;

import io.smallrye.stork.api.observability.EventCompletionHandler;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkResolutionEvent;

public class FakeObservationCollector implements ObservationCollector {

    private static final EventCompletionHandler FAKE_HANDLER = ev -> {
        // FAKE
    };
    public static StorkResolutionEvent FAKE_STORK_EVENT;

    @Override
    public StorkResolutionEvent create(String serviceName, String serviceDiscoveryType,
                                       String serviceSelectionType) {
        FAKE_STORK_EVENT = new StorkResolutionEvent(
                serviceName, serviceDiscoveryType, serviceSelectionType,
                FAKE_HANDLER);
        return FAKE_STORK_EVENT;
    }
}
