package examples;

import static examples.AcmeObservationCollector.*;

import java.time.Duration;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.observability.ObservationCollector;

public class ObservationExample {

    public static void example(Stork stork) {
        Service service = stork.getService("my-service");

        ObservationCollector observations = service.getObservations();

        // Gets the time spent in service discovery and service selection even if any error happens
        Duration overallDuration = ACME_STORK_EVENT.getOverallDuration();

        // Gets the total number of instances discovered
        int discoveredInstancesCount = ACME_STORK_EVENT.getDiscoveredInstancesCount();

        // Gets the error raised during the process
        Throwable failure = ACME_STORK_EVENT.failure();

        //        ...

    }
}
