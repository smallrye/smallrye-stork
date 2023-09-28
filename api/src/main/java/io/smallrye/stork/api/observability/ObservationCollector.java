package io.smallrye.stork.api.observability;

public interface ObservationCollector {

    StorkObservationPoints create(String serviceName, String serviceDiscoveryType, String serviceSelectionType);

}
