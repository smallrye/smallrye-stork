package io.smallrye.discovery;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.smallrye.config.access.ServiceConfigAccessor;

public class StaticListServiceDiscoveryProducer implements ServiceDiscoveryProducer {

    public static final String SERVICE_DISCOVERY_TYPE = "static";

    public static final String CONFIG_PREFIX = "service-discovery";

    private static final String CONFIG_KEY_TYPE = "type";

    private final ServiceConfigAccessor serviceConfigAccessor;

    public StaticListServiceDiscoveryProducer() {
        this(new ServiceConfigAccessor(CONFIG_PREFIX));
    }

    public StaticListServiceDiscoveryProducer(ServiceConfigAccessor serviceConfigAccessor) {
        this.serviceConfigAccessor = serviceConfigAccessor;
    }

    public StaticListServiceDiscovery getServiceDiscovery(String serviceName) {
        if (!SERVICE_DISCOVERY_TYPE.equals(serviceConfigAccessor.getValue(serviceName, CONFIG_KEY_TYPE))) {
            throw new IllegalArgumentException("Static list service discovery is not enabled for this service");
        }

        List<ServiceInstance> serviceInstances = new LinkedList<>();
        for (String id : getServiceIds(serviceName)) {
            String value = serviceConfigAccessor.getValue(serviceName, id);
            if (value != null) {
                serviceInstances.add(new ServiceInstance(id, value));
            }
        }

        return new StaticListServiceDiscovery(serviceInstances);
    }

    private List<String> getServiceIds(String serviceName) {
        return serviceConfigAccessor.getKeys(serviceName)
                .stream()
                .filter(key -> !CONFIG_KEY_TYPE.equals(key))
                .collect(Collectors.toList());
    }
}
