package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.loadbalancer.random.RandomConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;

import java.time.Duration;

public class InitializationExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();
        // ...
    }
}
