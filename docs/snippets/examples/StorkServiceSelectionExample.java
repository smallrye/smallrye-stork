package examples;

import io.smallrye.stork.Service;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;

import java.time.Duration;

public class StorkServiceSelectionExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();

        Service service = stork.getService("my-service");
        ServiceInstance instance = service.selectServiceInstance()
                .await().atMost(Duration.ofSeconds(5));

        System.out.println(instance.getHost() + ":" + instance.getPort());

        // ...
        Stork.shutdown();
    }

}
