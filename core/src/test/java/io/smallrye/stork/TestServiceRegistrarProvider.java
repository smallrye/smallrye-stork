package io.smallrye.stork;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarType(value = "test", metadataKey = TestMetadataKey.class)
public class TestServiceRegistrarProvider implements ServiceRegistrarProvider<TestRegistrarConfiguration, TestMetadataKey> {

    @Override
    public ServiceRegistrar createServiceRegistrar(TestRegistrarConfiguration config, String serviceRegistrarName,
            StorkInfrastructure infrastructure) {
        return new TestServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }
}
