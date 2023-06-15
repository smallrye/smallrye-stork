package io.smallrye.stork.servicediscovery.staticlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarType(value = "static", metadataKey = Metadata.DefaultMetadataKey.class)
public class StaticListServiceRegistrarProvider
        implements ServiceRegistrarProvider<StaticRegistrarConfiguration, Metadata.DefaultMetadataKey> {

    private static final Logger log = LoggerFactory.getLogger(StaticListServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<Metadata.DefaultMetadataKey> createServiceRegistrar(StaticRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new StaticListServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
