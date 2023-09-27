package io.smallrye.stork.api.observability;

import java.util.List;

import io.smallrye.stork.api.ServiceInstance;

public class NoopObservationCollector implements ObservationCollector {

    public static final StorkServiceMetrics NOOP_STORK_EVENT = new StorkServiceMetrics(
            null, null,
            null) {
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
    public StorkServiceMetrics create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        return NOOP_STORK_EVENT;
    }

}
