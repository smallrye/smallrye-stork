package io.smallrye.stork.serviceregistration.staticlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.InMemoryAddressesBackend;
import io.smallrye.stork.utils.StorkAddressUtils;

public class StaticListServiceRegistrar implements ServiceRegistrar<Metadata.DefaultMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(StaticListServiceRegistrar.class);
    private final StaticRegistrarConfiguration config;

    public StaticListServiceRegistrar(StaticRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        this.config = config;
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<Metadata.DefaultMetadataKey> metadata,
            String ipAddress,
            int defaultPort) {
        HostAndPort hostAndPortToAdd = StorkAddressUtils.parseToHostAndPort(ipAddress, defaultPort,
                "service '" + serviceName + "'");
        String hostAndPortToAddString = StorkAddressUtils.parseToString(hostAndPortToAdd);
        InMemoryAddressesBackend.add(serviceName, hostAndPortToAddString);
        return Uni.createFrom().voidItem();
    }

}
