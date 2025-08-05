package examples;

import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.StorkServiceRegistry;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;

public class AcmeRegistrarApiUsage {

    public void example(StorkServiceRegistry stork) {
        String list = "localhost:8080, localhost:8081";

        stork.defineIfAbsent("my-service", ServiceDefinition.of(
                new StaticConfiguration().withAddressList(list),
                new AcmeLoadBalancerConfiguration().withMyAttribute("my-value"),new AcmeRegistrarConfiguration())
        );

        stork.getService("my-service").registerInstance("my-service", "localhost",
                9000);
    }

}
