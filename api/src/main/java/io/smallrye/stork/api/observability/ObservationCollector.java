package io.smallrye.stork.api.observability;

public interface ObservationCollector {

    StorkObservation create(String serviceName, String serviceDiscoveryType, String serviceSelectionType);

}
