package io.smallrye.stork.servicediscovery.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class CompositeServiceDiscoveryProvider implements ServiceDiscoveryProvider {

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName,
            ServiceConfig serviceConfig) {
        // we're configuring service discovery for
        // config prefix stork.<service-name>.discovery
        // Service names for composite config should be listed as a comma separated list:
        // stork.<service-name>.discovery.services=serviceA,serviceB,serviceC
        Map<String, String> parameters = config.parameters();

        String serviceList = parameters.get("services");
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
        return new CompositeServiceDiscovery(serviceNames);
    }

    @Override
    public String type() {
        return "composite";
    }
}
