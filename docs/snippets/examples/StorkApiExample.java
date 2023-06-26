package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.loadbalancer.random.RandomConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticRegistrarConfiguration;

import java.time.Duration;

public class StorkApiExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();

        String example = "localhost:8080, localhost:8082";

        // A service using a static list of locations as discovery
        // As not set, it defaults to round-robin to select the instance.
        stork.defineIfAbsent("my-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example)));

        // Another service using the random selection strategy, instead of round-robin
        stork.defineIfAbsent("my-second-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example),
                        new RandomConfiguration()));

        ServiceInstance instance = stork.getService("my-second-service").selectInstance()
                .await().atMost(Duration.ofSeconds(1));
        System.out.println(instance.getHost() + ":" + instance.getPort());

        // Another service using the random selection strategy, instead of round-robin and a static service registrar
        stork.defineIfAbsent("my-third-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example),
                        new RandomConfiguration(), new StaticRegistrarConfiguration()));
    }
}
