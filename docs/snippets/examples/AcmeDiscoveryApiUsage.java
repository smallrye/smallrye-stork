package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.StorkServiceRegistry;

import java.util.List;

public class AcmeDiscoveryApiUsage {

    public void example(StorkServiceRegistry stork) {
        stork.defineIfAbsent("my-service", ServiceDefinition.of(
                new AcmeConfiguration().withHost("my-host"))
        );

        Uni<List<ServiceInstance>> serviceInstances = stork.getService("my-service").getInstances();
    }

}
