package io.smallrye.stork.servicediscovery.staticlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.servicediscovery.staticlist.StaticListServiceRegistrar.StaticAddressesBackend;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.StorkAddressUtils;

/**
 * A service discovery provider using a static list of service instances.
 */
@ServiceDiscoveryType("static")
@ServiceDiscoveryAttribute(name = "address-list", description = "A comma-separated list of addresses (host:port). The default port is 80.", required = true)
@ServiceDiscoveryAttribute(name = "secure", description = "Whether the connection with the service should be encrypted with TLS. Default is false, except if the host:port uses the port is 443.")
@ServiceDiscoveryAttribute(name = "shuffle", description = "Whether the list of address must be shuffled to avoid using the first address on every startup.", defaultValue = "false")
@ApplicationScoped
public class StaticListServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<StaticConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(StaticConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        String addresses = config.getAddressList();
        List<DefaultServiceInstance> addressList = new ArrayList<>();
        if (addresses != null && !addresses.isBlank()) {
            for (String address : addresses.split(",")) {
                address = address.trim();
                try {
                    HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address,
                            address.startsWith(StorkAddressUtils.HTTPS_PREFIX) ? 443 : 80,
                            "service '" + serviceName + "'");
                    addressList
                            .add(new DefaultServiceInstance(ServiceInstanceIds.next(), hostAndPort.host, hostAndPort.port,
                                    hostAndPort.path, isSecure(config.getSecure(), hostAndPort.port)));
                    StaticAddressesBackend.add(serviceName, address);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Address not parseable to URL: " + address + " for service " + serviceName);
                }
            }
            if (Boolean.parseBoolean(config.getShuffle())) {
                Collections.shuffle(addressList);
            }
        }

        return new StaticListServiceDiscovery(serviceName, addressList);
    }

    private boolean isSecure(String secureAttribute, int port) {
        if (secureAttribute == null) {
            return port == 443;
        } else {
            return Boolean.parseBoolean(secureAttribute);
        }

    }
}
