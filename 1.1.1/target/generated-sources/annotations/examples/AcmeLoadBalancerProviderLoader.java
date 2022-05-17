package examples;

import examples.AcmeLoadBalancerConfiguration;
import examples.AcmeLoadBalancerProvider;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.api.ServiceDiscovery;

/**
 * LoadBalancerLoader for examples.AcmeLoadBalancerProvider
 */
 public class AcmeLoadBalancerProviderLoader implements io.smallrye.stork.spi.internal.LoadBalancerLoader {
   private final examples.AcmeLoadBalancerProvider provider = new examples.AcmeLoadBalancerProvider();
   @Override
   public LoadBalancer createLoadBalancer(LoadBalancerConfig config, ServiceDiscovery serviceDiscovery) {
      examples.AcmeLoadBalancerConfiguration typedConfig = new examples.AcmeLoadBalancerConfiguration(config.parameters());
      return provider.createLoadBalancer(typedConfig, serviceDiscovery);
   }

  /**
   * @return the type
   */
   @Override
   public String type() {
      return "acme-load-balancer";
   }
}
