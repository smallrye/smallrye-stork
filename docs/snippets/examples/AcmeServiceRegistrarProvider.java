package examples;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import jakarta.enterprise.context.ApplicationScoped;

@ServiceRegistrarType(value = "acme", metadataKey = Metadata.DefaultMetadataKey.class)
@ServiceRegistrarAttribute(name = "host",
        description = "Host name of the service discovery server.", required = true)
@ServiceRegistrarAttribute(name = "port",
        description = "Hort of the service discovery server.", required = false)
@ApplicationScoped
public class AcmeServiceRegistrarProvider
        implements ServiceRegistrarProvider<AcmeRegistrarConfiguration, Metadata.DefaultMetadataKey> {

    @Override
    public ServiceRegistrar createServiceRegistrar(
            AcmeRegistrarConfiguration config,
            String serviceName,
            StorkInfrastructure storkInfrastructure) {
        return new AcmeServiceRegistrar(config);
    }

}
