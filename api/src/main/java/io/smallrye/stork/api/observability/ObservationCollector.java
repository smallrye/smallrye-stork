package io.smallrye.stork.api.observability;

public interface ObservationCollector {

    ObservationPoints.StorkResolutionEvent create(String serviceName, String serviceDiscoveryType, String serviceSelectionType);

    interface EventCompletionHandler {
        public void complete(ObservationPoints.StorkResolutionEvent event);
    }
}
