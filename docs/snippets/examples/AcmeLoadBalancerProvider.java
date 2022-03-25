package examples;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.LoadBalancerAttribute;
import io.smallrye.stork.api.config.LoadBalancerType;
import io.smallrye.stork.spi.LoadBalancerProvider;

@LoadBalancerType("acme-load-balancer")
@LoadBalancerAttribute(name = "my-attribute",
        description = "Attribute that alters the behavior of the LoadBalancer")
public class AcmeLoadBalancerProvider implements
        LoadBalancerProvider<AcmeLoadBalancerConfiguration> {

    @Override
    public LoadBalancer createLoadBalancer(AcmeLoadBalancerConfiguration config,
                                           ServiceDiscovery serviceDiscovery) {
        return new AcmeLoadBalancer(config);
    }
}
