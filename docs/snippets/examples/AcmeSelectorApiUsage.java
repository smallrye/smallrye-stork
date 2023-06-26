package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.StorkServiceRegistry;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticRegistrarConfiguration;

public class AcmeSelectorApiUsage {

    public void example(StorkServiceRegistry stork) {
        String list = "localhost:8080, localhost:8081";
        stork.defineIfAbsent("my-service", ServiceDefinition.of(
                new StaticConfiguration().withAddressList(list),
                new AcmeLoadBalancerConfiguration().withMyAttribute("my-value"),new StaticRegistrarConfiguration())
        );

        Uni<ServiceInstance> uni = stork.getService("my-service").selectInstance();
    }

}
