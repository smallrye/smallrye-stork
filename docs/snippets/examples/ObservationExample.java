package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkObservation;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static examples.AcmeObservationCollector.*;

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
