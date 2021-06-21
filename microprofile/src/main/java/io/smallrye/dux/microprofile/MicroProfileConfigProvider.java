package io.smallrye.dux.microprofile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.smallrye.dux.config.DuxConfigProvider;
import io.smallrye.dux.config.ServiceConfig;
import io.smallrye.dux.spi.SimpleServiceConfig;

public class MicroProfileConfigProvider implements DuxConfigProvider {

    private static final Logger log = Logger.getLogger(MicroProfileConfigProvider.class);

    public static final String DUX = "dux";
    public static final String LOAD_BALANCER = "load-balancer";
    public static final String SERVICE_DISCOVERY = "service-discovery";
    private final List<ServiceConfig> serviceConfigs = new ArrayList<>();

    public MicroProfileConfigProvider() {
        Config config = ConfigProvider.getConfig();

        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();

        for (String propertyName : config.getPropertyNames()) {
            String[] property = propertyName.split("\\.");
            if (property.length < 1 || !property[0].equals(DUX)) {
                continue;
            }

            // all properties are now of form
            // dux.<service-name>.(load-balancer|service-discovery)...
            if (property.length < 3) {
                log.warn("Potentially invalid property for SmallRye Dux: " + propertyName);
            }

            String serviceName = property[1];
            Map<String, String> serviceProperties = propertiesByServiceName.computeIfAbsent(serviceName,
                    ignored -> new HashMap<>());

            serviceProperties.put(propertyName.substring(property[0].length() + property[1].length() + 2),
                    config.getValue(propertyName, String.class));
        }

        for (Map.Entry<String, Map<String, String>> serviceEntry : propertiesByServiceName.entrySet()) {
            SimpleServiceConfig.Builder builder = new SimpleServiceConfig.Builder();

            Map<String, String> properties = serviceEntry.getValue();
            String loadBalancerType = properties.get(LOAD_BALANCER);
            builder.setServiceName(serviceEntry.getKey());
            if (loadBalancerType != null) {
                SimpleServiceConfig.SimpleLoadBalancerConfig loadBalancer = new SimpleServiceConfig.SimpleLoadBalancerConfig(
                        loadBalancerType, propertiesForPrefix(LOAD_BALANCER, properties));

                builder = builder.setLoadBalancer(loadBalancer);
            }

            String serviceDiscoveryType = properties.get(SERVICE_DISCOVERY);
            if (serviceDiscoveryType != null) {
                SimpleServiceConfig.SimpleServiceDiscoveryConfig serviceDiscovery = new SimpleServiceConfig.SimpleServiceDiscoveryConfig(
                        serviceDiscoveryType, propertiesForPrefix(SERVICE_DISCOVERY, properties));

                builder = builder.setServiceDiscovery(serviceDiscovery);
            }

            serviceConfigs.add(builder.build());
        }
    }

    private Map<String, String> propertiesForPrefix(String prefix, Map<String, String> original) {
        prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : original.entrySet()) {
            String propertyName = entry.getKey();
            if (propertyName.startsWith(prefix)) {
                String nameWithoutPrefix = propertyName.substring(prefix.length());
                result.put(nameWithoutPrefix, entry.getValue());
            }
        }

        return result;
    }

    @Override
    public List<ServiceConfig> getDuxConfigs() {
        return serviceConfigs;
    }

    @Override
    public int priority() {
        return 100;
    }

}
