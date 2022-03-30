package io.smallrye.stork.servicediscovery.staticlist;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.impl.DefaultServiceInstance;
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
public class StaticListServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<StaticConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(StaticConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        String addresses = config.getAddressList();
        if (addresses == null || addresses.isBlank()) {
            throw new IllegalArgumentException("No address list defined for service " + serviceName);
        }
        List<DefaultServiceInstance> addressList = new ArrayList<>();
        for (String address : addresses.split(",")) {
            URL url = null;
            address = address.trim();
            try {
                HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address, 80, serviceName);
                addressList
                        .add(new DefaultServiceInstance(ServiceInstanceIds.next(), hostAndPort.host, hostAndPort.port,
                                isSecure(config.getSecure(), hostAndPort.port)));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Address not parseable to URL: " + url + " for service " + serviceName);
            }
        }

        return new StaticListServiceDiscovery(addressList);
    }

    private boolean isSecure(String secureAttribute, int port) {
        if (secureAttribute == null) {
            return port == 443;
        } else {
            return Boolean.parseBoolean(secureAttribute);
        }

    }
}
