package io.smallrye.stork.api.observability;

public interface ObservationCollector {

    StorkServiceMetrics create(String serviceName, String serviceDiscoveryType, String serviceSelectionType);

}
