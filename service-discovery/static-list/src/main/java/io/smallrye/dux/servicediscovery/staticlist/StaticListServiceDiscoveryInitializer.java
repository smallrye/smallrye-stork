package io.smallrye.dux.servicediscovery.staticlist;

import static io.smallrye.dux.config.ConfigUtils.keySegment;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceDiscoveryHandler;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.config.ConfigAccessor;

public class StaticListServiceDiscoveryInitializer {

    private static final String SERVICE_DISCOVERY_TYPE = "static";

    private static final String CONFIG_PREFIX = "service-discovery";

    private static final String TYPE_KEY_SEGMENT = "type";

    private static final int SERVICE_NAME_SEGMENT_POSITION = 1;

    private static final int SERVICE_ID_SEGMENT_POSITION = 2;

    private final ConfigAccessor configAccessor;

    public StaticListServiceDiscoveryInitializer() {
        this.configAccessor = new ConfigAccessor();
    }

    public StaticListServiceDiscoveryInitializer(Config config) {
        this.configAccessor = new ConfigAccessor(config);
    }

    public void init(ServiceDiscovery serviceDiscovery) {
        for (String serviceName : getServiceNames()) {
            serviceDiscovery.registerServiceDiscoveryHandler(getHandler(serviceName));
        }
    }

    private ServiceDiscoveryHandler getHandler(String serviceName) {
        List<ServiceInstance> serviceInstances = new LinkedList<>();

        for (String id : getServiceIds(serviceName)) {
            serviceInstances.add(getServiceInstance(serviceName, id));
        }

        return new StaticListServiceDiscoveryHandler(serviceName, serviceInstances);
    }

    private Set<String> getServiceNames() {
        Set<String> serviceNames = new HashSet<>();

        for (String key : configAccessor.getKeys(CONFIG_PREFIX)) {
            String serviceName = keySegment(key, SERVICE_NAME_SEGMENT_POSITION);
            if (isStaticServiceDiscoveryService(serviceName)) {
                serviceNames.add(serviceName);
            }
        }

        return serviceNames;
    }

    private Set<String> getServiceIds(String serviceName) {
        String servicePrefix = String.format("%s.%s", CONFIG_PREFIX, serviceName);
        Set<String> serviceIds = new HashSet<>();

        for (String key : configAccessor.getKeys(servicePrefix)) {
            String serviceId = keySegment(key, SERVICE_ID_SEGMENT_POSITION);
            if (!TYPE_KEY_SEGMENT.equals(serviceId)) {
                serviceIds.add(serviceId);
            }
        }

        return serviceIds;
    }

    private ServiceInstance getServiceInstance(String serviceName, String id) {
        String serviceInstancePrefix = String.format("%s.%s.%s", CONFIG_PREFIX, serviceName, id);
        String value = configAccessor.getValue(serviceInstancePrefix);

        return new ServiceInstance(id, value);
    }

    private boolean isStaticServiceDiscoveryService(String serviceName) {
        return SERVICE_DISCOVERY_TYPE.equals(
                configAccessor.getValue(String.format("%s.%s.%s", CONFIG_PREFIX, serviceName, TYPE_KEY_SEGMENT)));
    }
}
