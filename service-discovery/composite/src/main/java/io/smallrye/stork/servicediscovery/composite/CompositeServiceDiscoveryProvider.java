package io.smallrye.stork.servicediscovery.composite;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryAttribute;
import io.smallrye.stork.api.config.ServiceDiscoveryType;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceDiscoveryType("composite")
@ServiceDiscoveryAttribute(name = "services", description = "A comma-separated list of services that this services consists of.", required = true)
public class CompositeServiceDiscoveryProvider
        implements ServiceDiscoveryProvider<CompositeServiceDiscoveryProviderConfiguration> {

    @Override
    public ServiceDiscovery createServiceDiscovery(CompositeServiceDiscoveryProviderConfiguration config, String serviceName,
            ServiceConfig serviceConfig, StorkInfrastructure storkInfrastructure) {
        // Service names for composite config should be listed as a comma separated list:
        // stork.<service-name>.discovery.services=serviceA,serviceB,serviceC
        String serviceList = config.getServices();
        if (serviceList == null) {
            throw new IllegalArgumentException("'services' property missing for service '" + serviceName + "'. " +
                    "Please provide a comma separated list of service names.");
        }

        String[] services = serviceList.split(",");

        List<String> serviceNames = new ArrayList<>();
        for (String service : services) {
            service = service.trim();
            if (service.isBlank()) {
                throw new IllegalArgumentException("Blank constituent service name for service '" + serviceName + "'");
            }
            serviceNames.add(service);
        }
        return new CompositeServiceDiscovery(serviceName, serviceNames);
    }
}
