package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.smallrye.loadbalancer.core.ConfigUtils.getConfig;

// todo names
// todo move to another module?
public class ReflectionProviderProducerFactory implements ProviderProducerFactory {

    private final Map<String, TargetAddressProviderProducer> addressProviders = new ConcurrentHashMap<>();

    @Override
    public TargetAddressProvider get(String name) {
        Config loadBalancerConfig = getConfig(name);

        String providerType = loadBalancerConfig.getValue("address-provider", String.class);


        if (providerType == null) {
            throw new IllegalArgumentException("No address-provider defined for load balancer  " + name);
        }

        TargetAddressProviderProducer providerProducer = addressProviders.computeIfAbsent(providerType, this::createProviderProducer);

        return providerProducer.getTargetAddressProvider(loadBalancerConfig, name);
    }

    private TargetAddressProviderProducer createProviderProducer(String providerType) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> producerClass;
        try {
            producerClass = classLoader.loadClass(providerType);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No TargetAddressProviderProducer class found: " + providerType, e);
        }
        try {
            return (TargetAddressProviderProducer) producerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to instantiate TargetAddressProviderProducer " + providerType, e);
        }
    }
}
