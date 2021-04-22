package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.smallrye.loadbalancer.core.ConfigUtils.getConfig;

// todo names
// todo move to another module?
public class ReflectionLoadBalancerFactory implements LoadBalancerFactory {

    private final Map<String, TargetAddressProviderProducer> addressProviders = new ConcurrentHashMap<>();

    @Override
    public LoadBalancer get(String name) {
        Config loadBalancerConfig = getConfig(name);

        String providerType = loadBalancerConfig.getValue("address-provider", String.class);
        if (providerType == null) {
            throw new IllegalArgumentException("No address-provider defined for load balancer  " + name);
        }

        TargetAddressProviderProducer providerProducer = addressProviders.computeIfAbsent(providerType,
                className -> instantiateClass(className, TargetAddressProviderProducer.class));

        TargetAddressProvider targetAddressProvider = providerProducer.getTargetAddressProvider(loadBalancerConfig, name);

        String loadBalancerType = loadBalancerConfig.getValue("type", String.class);
        LoadBalancerProducer loadBalancerProducer = instantiateClass(loadBalancerType, LoadBalancerProducer.class);

        return loadBalancerProducer.getLoadBalancer(targetAddressProvider, loadBalancerConfig);
    }

    private <T> T instantiateClass(String providerType, Class<T> clazz) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> producerClass;
        try {
            producerClass = classLoader.loadClass(providerType);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No class found: " + providerType, e);
        }
        try {
            Object result = producerClass.getDeclaredConstructor().newInstance();
            if (clazz.isInstance(result)) {
                //noinspection unchecked
                return (T) result;
            } else {
                throw new IllegalArgumentException("Expected an instance of " + clazz.getName() + ", found " + result.getClass());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to instantiate " + providerType, e);
        }
    }
}
