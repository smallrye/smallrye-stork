package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.loadbalancer.random.RandomConfiguration;
import io.smallrye.stork.servicediscovery.consul.ConsulRegistrarConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticRegistrarConfiguration;

public class DefinitionExample {

    public static void example(Stork stork) {
        String example = "localhost:8080, localhost:8081";

        // A service using a static list of locations as discovery
        // As not set, it defaults to round-robin to select the instance.
        stork.defineIfAbsent("my-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example)));

        // Another service using the random selection strategy, instead of round-robin
        stork.defineIfAbsent("my-second-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example),
                        new RandomConfiguration()));

        // Another service using the random selection strategy, instead of round-robin and a static service registrar
        stork.defineIfAbsent("my-second-service",
                ServiceDefinition.of(new StaticConfiguration().withAddressList(example),
                        new RandomConfiguration(), new StaticRegistrarConfiguration()));
    }
}
