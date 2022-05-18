package examples;

import examples.AcmeConfiguration;
import examples.AcmeServiceDiscoveryProvider;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.StorkInfrastructure;

/**
 * ServiceDiscoveryLoader for {@link examples.AcmeServiceDiscoveryProvider}
 */
 public class AcmeServiceDiscoveryProviderLoader implements io.smallrye.stork.spi.internal.ServiceDiscoveryLoader {
   private final examples.AcmeServiceDiscoveryProvider provider = new examples.AcmeServiceDiscoveryProvider();
   @Override
   public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
              ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
      examples.AcmeConfiguration typedConfig = new examples.AcmeConfiguration(config.parameters());
      return provider.createServiceDiscovery(typedConfig, serviceName, serviceConfig, storkInfrastructure);
   }

  /**
   * @return the type
   */
   @Override
   public String type() {
      return "acme";
   }
}
