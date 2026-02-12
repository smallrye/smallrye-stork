package io.smallrye.stork.serviceregistration.staticlist;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.InMemoryAddressesBackend;
import io.smallrye.stork.utils.StorkAddressUtils;

public class StaticListServiceRegistrar implements ServiceRegistrar<Metadata.DefaultMetadataKey> {
    private static final Logger log = Logger.getLogger(StaticListServiceRegistrar.class);
    private final StaticRegistrarConfiguration config;

    public StaticListServiceRegistrar(StaticRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        this.config = config;
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<Metadata.DefaultMetadataKey> metadata,
            String ipAddress,
            int defaultPort) {
        checkAddressNotNull(ipAddress);
        HostAndPort hostAndPortToAdd = StorkAddressUtils.parseToHostAndPort(ipAddress, defaultPort,
                "service '" + serviceName + "'");
        String hostAndPortToAddString = StorkAddressUtils.parseToString(hostAndPortToAdd);
        InMemoryAddressesBackend.add(serviceName, hostAndPortToAddString);
        log.infof("Address %s has been registered for service %s", hostAndPortToAddString, serviceName);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        InMemoryAddressesBackend.clear(serviceName);
        return Uni.createFrom().voidItem();
    }

}
