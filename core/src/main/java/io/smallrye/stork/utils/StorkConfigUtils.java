package io.smallrye.stork.utils;

import static io.smallrye.stork.Stork.STORK;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.smallrye.stork.Stork;
import io.smallrye.stork.spi.config.SimpleServiceConfig;

public class StorkConfigUtils {

    private static final Logger log = Logger.getLogger(StorkConfigUtils.class);

    private static final String CONFIG_PROPERTY_PART_EXPRESSION = "\".*\"|[^.]+";
    private static final Pattern CONFIG_PROP_PART = Pattern.compile(CONFIG_PROPERTY_PART_EXPRESSION);

    /**
     * The load-balancer segment used in the configuration key.
     */
    public static final String LOAD_BALANCER = "load-balancer";
    /**
     * The load balancer type configuration key.
     */
    public static final String LOAD_BALANCER_EMBEDDED = "load-balancer.type";

    /**
     * The service-discovery segment used in the configuration key.
     */
    public static final String SERVICE_DISCOVERY = "service-discovery";
    /**
     * The service discovery type configuration key.
     */
    public static final String SERVICE_DISCOVERY_EMBEDDED = "service-discovery.type";

    /**
     * The service-registrar segment used in the configuration key.
     */
    public static final String SERVICE_REGISTRAR = "service-registrar";
    /**
     * The service discovery type configuration key.
     */
    public static final String SERVICE_REGISTRAR_EMBEDDED = "service-registrar.type";

    /**
     * Computes and stores service properties based on the provided property name and value.
     *
     * <p>
     * This method processes a property name and value, extracting relevant information and storing it in a map
     * based on the service name. The property name is expected to follow a specific pattern associated with SmallRye Stork,
     * and it's used to determine the service name and property key. If the property name does not match the expected
     * pattern, it will be logged as a warning.
     *
     * @param propertiesByServiceName A map containing service properties indexed by service name.
     * @param propertyName The name of the property to compute.
     * @param propertyValue The value of the property to store.
     */
    public static void computeServiceProperty(Map<String, Map<String, String>> propertiesByServiceName, String propertyName,
            String propertyValue) {

        Matcher matcher = CONFIG_PROP_PART.matcher(propertyName);

        if (!matcher.find()) {
            return;
        }
        if (STORK.equals(matcher.group())) {

            // all registry and load balancing properties are of form
            // stork.<service-name>.(load-balancer|service-discovery)...
            // or stork."<service-name>".(load-balancer|service-discovery)...
            if (!matcher.find()) {
                log.warn("Potentially invalid property for SmallRye Stork: " + propertyName);
            }
            String serviceName = unwrapFromQuotes(matcher.group());

            int serviceNameEndIdx = matcher.end();

            if (!matcher.find()) {
                log.warn("Potentially invalid property for SmallRye Stork: " + propertyName);
            }

            // serviceName can be in double quotes
            Map<String, String> serviceProperties = propertiesByServiceName.computeIfAbsent(serviceName,
                    ignored -> new HashMap<>());

            String serviceProperty = propertyKey(propertyName.substring(serviceNameEndIdx));
            serviceProperties.put(serviceProperty, propertyValue);
        }

    }

    public static SimpleServiceConfig buildServiceConfig(Map.Entry<String, Map<String, String>> serviceEntry) {
        SimpleServiceConfig.Builder builder = new SimpleServiceConfig.Builder();

        Map<String, String> properties = serviceEntry.getValue();

        String serviceName = serviceEntry.getKey();

        String loadBalancerType = properties.get(LOAD_BALANCER);
        if (loadBalancerType == null) {
            loadBalancerType = properties.get(LOAD_BALANCER_EMBEDDED);
        }
        builder.setServiceName(serviceName);
        if (loadBalancerType != null) {
            SimpleServiceConfig.SimpleLoadBalancerConfig loadBalancerConfig = new SimpleServiceConfig.SimpleLoadBalancerConfig(
                    loadBalancerType, propertiesForPrefix(LOAD_BALANCER, properties));

            builder = builder.setLoadBalancer(loadBalancerConfig);
        }

        String serviceDiscoveryType = properties.get(SERVICE_DISCOVERY);
        // for yaml it is more convenient to have stork.my-service.service-discovery.type, so let's support it too:
        if (serviceDiscoveryType == null) {
            serviceDiscoveryType = properties.get(SERVICE_DISCOVERY_EMBEDDED);
        }
        if (serviceDiscoveryType != null) {
            SimpleServiceConfig.SimpleServiceDiscoveryConfig serviceDiscoveryConfig = new SimpleServiceConfig.SimpleServiceDiscoveryConfig(
                    serviceDiscoveryType, propertiesForPrefix(SERVICE_DISCOVERY, properties));

            builder = builder.setServiceDiscovery(serviceDiscoveryConfig);
        }

        String serviceRegistrarType = properties.get(SERVICE_REGISTRAR);
        if (serviceRegistrarType == null) {
            serviceRegistrarType = properties.get(SERVICE_REGISTRAR_EMBEDDED);
        }
        if (serviceRegistrarType != null) {
            SimpleServiceConfig.SimpleServiceRegistrarConfig serviceRegistrarConfig = new SimpleServiceConfig.SimpleServiceRegistrarConfig(
                    serviceRegistrarType, propertiesForPrefix(SERVICE_REGISTRAR, properties));

            builder = builder.setServiceRegistrar(serviceRegistrarConfig);
        }
        return builder.build();
    }

    public static String unwrapFromQuotes(String text) {
        if (text.length() < 2) {
            return text;
        }
        if (text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            return text.substring(1, text.length() - 1);
        } else {
            return text;
        }
    }

    public static String propertyKey(String text) {
        if (!text.isEmpty() && text.charAt(0) == '.') {
            return text.substring(1);
        }
        return text;
    }

    public static Map<String, String> propertiesForPrefix(String prefix, Map<String, String> original) {
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
}
