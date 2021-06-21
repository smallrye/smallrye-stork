package io.smallrye.dux;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.smallrye.dux.config.DuxConfigProvider;
import io.smallrye.dux.config.ServiceConfig;
import io.smallrye.dux.spi.ElementWithType;
import io.smallrye.dux.spi.LoadBalancerProvider;
import io.smallrye.dux.spi.ServiceDiscoveryProvider;

// TODO replace all the exceptions here with dedicated ones?
public final class Dux {

    private final Map<String, ServiceDiscovery> serviceDiscoveries = new ConcurrentHashMap<>();

    private final Map<String, LoadBalancer> loadBalancers = new ConcurrentHashMap<>();

    public ServiceDiscovery getServiceDiscovery(String serviceName) {
        ServiceDiscovery serviceDiscovery = serviceDiscoveries.get(serviceName);
        if (serviceDiscovery == null) {
            // TODO dedicated exception?
            throw new IllegalArgumentException("No service discovery defined for service name " + serviceName);
        }
        return serviceDiscovery;
    }

    public LoadBalancer getLoadBalancer(String serviceName) {
        LoadBalancer loadBalancer = loadBalancers.get(serviceName);
        if (loadBalancer == null) {
            // TODO dedicated exception?
            throw new IllegalArgumentException("No load balancer defined for service name " + serviceName);
        }
        return loadBalancer;
    }

    @Deprecated // for tests only
    Dux() {
        Map<String, LoadBalancerProvider> loadBalancerProviders = getAll(LoadBalancerProvider.class);
        Map<String, ServiceDiscoveryProvider> serviceDiscoveryProviders = getAll(ServiceDiscoveryProvider.class);

        ServiceLoader<DuxConfigProvider> configs = ServiceLoader.load(DuxConfigProvider.class);
        // mstodo test for multiple config providers!!
        Optional<DuxConfigProvider> highestPrioConfigProvider = configs.stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(DuxConfigProvider::priority));

        DuxConfigProvider configProvider = highestPrioConfigProvider.orElseThrow(
                () -> new IllegalStateException("No DuxConfigProvider found"));

        for (ServiceConfig serviceConfig : configProvider.getDuxConfigs()) {
            final var serviceDiscoveryConfig = serviceConfig.serviceDiscovery();
            if (serviceDiscoveryConfig == null) {
                throw new IllegalArgumentException(
                        "No service discovery not defined for service " + serviceConfig.serviceName());
            }
            String serviceDiscoveryType = serviceDiscoveryConfig.type();
            if (serviceDiscoveryType == null) {
                throw new IllegalArgumentException(
                        "Service discovery type not defined for service " + serviceConfig.serviceName());
            }

            final var serviceDiscoveryProvider = serviceDiscoveryProviders.get(serviceDiscoveryType);
            if (serviceDiscoveryProvider == null) {
                throw new IllegalArgumentException("ServiceDiscoveryProvider not found for type " + serviceDiscoveryType);
            }

            final var serviceDiscovery = serviceDiscoveryProvider.createServiceDiscovery(serviceDiscoveryConfig);
            serviceDiscoveries.put(serviceConfig.serviceName(), serviceDiscovery);

            final var loadBalancerConfig = serviceConfig.loadBalancer();
            if (loadBalancerConfig == null) {
                // no load balancer, maybe someone intends to use service discovery only, ignoring
                // TODO: log debug sth
            } else {
                String loadBalancerType = loadBalancerConfig.type();
                final var loadBalancerProvider = loadBalancerProviders.get(loadBalancerType);
                if (loadBalancerProvider == null) {
                    throw new IllegalArgumentException("No LoadBalancerProvider for type " + loadBalancerType);
                }

                final var loadBalancer = loadBalancerProvider.createLoadBalancer(loadBalancerConfig, serviceDiscovery);
                loadBalancers.put(serviceConfig.serviceName(), loadBalancer);
            }
        }
    }

    private <T extends ElementWithType> Map<String, T> getAll(Class<T> providerClass) {
        ServiceLoader<T> providers = ServiceLoader.load(providerClass);
        return providers.stream().map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(ElementWithType::type, Function.identity()));
    }

    private static final Dux dux = new Dux();

    public static Dux getInstance() {
        return dux;
    }
}
