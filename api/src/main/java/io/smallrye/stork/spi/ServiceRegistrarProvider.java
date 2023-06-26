package io.smallrye.stork.spi;

import io.smallrye.stork.api.MetadataKey;
import io.smallrye.stork.api.ServiceRegistrar;

public interface ServiceRegistrarProvider<T, MetadataKeyType extends Enum<MetadataKeyType> & MetadataKey> {
    ServiceRegistrar<MetadataKeyType> createServiceRegistrar(T config, String serviceRegistrarName,
            StorkInfrastructure infrastructure);
}
