package examples;

import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;

public class LookupExample {

    public static void example(Stork stork) {
        Service service = stork.getService("my-service");

        // Gets all the available instances:
        Uni<List<ServiceInstance>> instances = service.getInstances();
        // Select one instance using the load balancing strategy
        Uni<ServiceInstance> instance = service.selectInstance();

        // Gets all the managed services:
        Map<String, Service> services = stork.getServices();
    }
}
