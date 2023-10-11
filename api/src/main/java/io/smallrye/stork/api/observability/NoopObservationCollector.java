package io.smallrye.stork.api.observability;

import java.util.List;

import io.smallrye.stork.api.ServiceInstance;

public class NoopObservationCollector implements ObservationCollector {

    private static final StorkEventHandler NOOP_HANDLER = ev -> {
        // NOOP
    };

    public static final StorkObservation NOOP_STORK_EVENT = new StorkObservation(
            null, null,
            null, NOOP_HANDLER) {
        @Override
        public void onServiceDiscoverySuccess(List<ServiceInstance> instances) {
            // Noop
        }

        @Override
        public void onServiceDiscoveryFailure(Throwable throwable) {
            // Noop
        }

        @Override
        public void onServiceSelectionSuccess(long id) {
            // Noop
        }

        @Override
        public void onServiceSelectionFailure(Throwable throwable) {
            // Noop
        }
    };

    @Override
    public StorkObservation create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        return NOOP_STORK_EVENT;
    }

}
