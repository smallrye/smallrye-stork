package io.smallrye.stork.integration;

import io.smallrye.stork.api.observability.ObservationCollector;

public class ObservableStorkInfrastructure extends DefaultStorkInfrastructure {

    private final ObservationCollector observationCollector;

    public ObservableStorkInfrastructure(ObservationCollector observationCollector) {
        this.observationCollector = observationCollector;
    }

    @Override
    public ObservationCollector getObservationCollector() {
        return observationCollector;
    }
}
