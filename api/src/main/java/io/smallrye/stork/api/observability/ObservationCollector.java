package io.smallrye.stork.api.observability;

public interface ObservationCollector {

    StorkResolutionEvent create(String serviceName, String serviceDiscoveryType, String serviceSelectionType);

}
