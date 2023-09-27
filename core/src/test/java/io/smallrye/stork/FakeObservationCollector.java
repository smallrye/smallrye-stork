package io.smallrye.stork;

import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkServiceMetrics;

public class FakeObservationCollector implements ObservationCollector {

    public static StorkServiceMetrics FAKE_STORK_EVENT;

    @Override
    public StorkServiceMetrics create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        FAKE_STORK_EVENT = new StorkServiceMetrics(
                serviceName, serviceDiscoveryType, serviceSelectionType);
        return FAKE_STORK_EVENT;
    }
}
