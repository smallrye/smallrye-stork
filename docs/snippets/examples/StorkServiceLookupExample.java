package examples;

import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;

import java.time.Duration;
import java.util.List;

public class StorkServiceLookupExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();

        Service service = stork.getService("my-service");
        List<ServiceInstance> instances = service.getServiceInstances()
                .await().atMost(Duration.ofSeconds(5));

        // ...
        Stork.shutdown();
    }

}
