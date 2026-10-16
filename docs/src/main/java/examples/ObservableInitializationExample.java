package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.integration.ObservableStorkInfrastructure;

public class ObservableInitializationExample {

    public static void main(String[] args) {
        Stork.initialize(new ObservableStorkInfrastructure(new AcmeObservationCollector()));
        Stork stork = Stork.getInstance();
        // ...
    }
}
