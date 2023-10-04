package io.smallrye.stork;

import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkEventHandler;
import io.smallrye.stork.api.observability.StorkObservation;

public class FakeObservationCollector implements ObservationCollector {

    private static final StorkEventHandler FAKE_HANDLER = ev -> {
        // FAKE
    };

    public static StorkObservation FAKE_STORK_EVENT;

    @Override
    public StorkObservation create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        FAKE_STORK_EVENT = new StorkObservation(
                serviceName, serviceDiscoveryType, serviceSelectionType,
                FAKE_HANDLER);
        return FAKE_STORK_EVENT;
    }
}
