package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkEventHandler;
import io.smallrye.stork.api.observability.StorkObservation;
import org.jboss.logging.Logger;

public class AcmeObservationCollector implements ObservationCollector {

    private static final Logger LOGGER = Logger.getLogger(AcmeObservationCollector.class);

    private static final StorkEventHandler ACME_HANDLER = event -> {
        //This is the terminal event. Put here your custom logic to extend the metrics collection.

        //E.g. Expose metrics to Micrometer, additional logs....
        LOGGER.info( "Service discovery took " + event.getServiceDiscoveryDuration() + ".");
        LOGGER.info( event.getDiscoveredInstancesCount() + " have been discovered for " + event.getServiceName() + ".");
        LOGGER.info( "Service selection took " + event.getServiceSelectionDuration() + ".");

        //        ...

    };

    public static StorkObservation ACME_STORK_EVENT;

    @Override
    public StorkObservation create(String serviceName, String serviceDiscoveryType,
                                   String serviceSelectionType) {
        ACME_STORK_EVENT = new StorkObservation(
                serviceName, serviceDiscoveryType, serviceSelectionType,
                ACME_HANDLER);
        return ACME_STORK_EVENT;
    }
}
