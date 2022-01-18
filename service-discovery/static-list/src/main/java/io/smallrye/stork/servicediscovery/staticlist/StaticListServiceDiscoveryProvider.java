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

@ServiceDiscoveryType("static")
@ServiceDiscoveryAttribute(name = "address-list", description = "a comma-separated list of addresses")
public class StaticListServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<StaticListServiceDiscoveryProviderConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(StaticListServiceDiscoveryProviderConfiguration config, String serviceName,
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
                                serviceConfig.secure()));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Address not parseable to URL: " + url + " for service " + serviceName);
            }
        }

        return new StaticListServiceDiscovery(addressList);
    }
}
