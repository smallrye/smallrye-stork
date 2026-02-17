package io.smallrye.stork.serviceregistration.staticlist;

import org.jboss.logging.Logger;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.InMemoryAddressesBackend;
import io.smallrye.stork.utils.StorkAddressUtils;

@ServiceRegistrarType(value = "static", metadataKey = Metadata.DefaultMetadataKey.class)
@ServiceRegistrarAttribute(name = "address-list", description = "A comma-separated list of addresses (host:port). The default port is 80.", required = true)
public class StaticListServiceRegistrarProvider
        implements ServiceRegistrarProvider<StaticRegistrarConfiguration, Metadata.DefaultMetadataKey> {

    private static final Logger log = Logger.getLogger(StaticListServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<Metadata.DefaultMetadataKey> createServiceRegistrar(StaticRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        String addresses = config.getAddressList();
        if (addresses != null && !addresses.isBlank()) {
            for (String address : addresses.split(",")) {
                address = address.trim();
                try {
                    HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address, 80,
                            "service '" + serviceRegistrarName + "'");
                    InMemoryAddressesBackend.add(serviceRegistrarName, address);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Address not parseable to URL: " + address + " for service " + serviceRegistrarName);
                }
            }
        }
        return new StaticListServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
