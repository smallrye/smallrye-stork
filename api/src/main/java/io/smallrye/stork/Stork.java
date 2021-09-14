package io.smallrye.stork;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.config.ConfigProvider;
import io.smallrye.stork.config.ServiceConfig;
import io.smallrye.stork.spi.ElementWithType;
import io.smallrye.stork.spi.LoadBalancerProvider;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

/**
 * The entrypoint for SmallRye Stork
 * <p>
 * Use `Stork.getInstance()` to get a hold of a configured instance, and retrieve `ServiceDiscovery` and/or `LoadBalancer`
 * from it.
 */
public final class Stork {
    // TODO replace all the exceptions here with dedicated ones?

    public static final String STORK = "stork";

    private static final Logger LOGGER = LoggerFactory.getLogger(Stork.class);

    private final Map<String, Service> services = new ConcurrentHashMap<>();

    public Service getService(String serviceName) {
        Service service = services.get(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("No service defined for name " + serviceName);
        }
        return service;
    }

    public Optional<Service> getServiceOptional(String serviceName) {
        Service service = services.get(serviceName);
        return Optional.ofNullable(service);
    }

    /**
     * Exposed for tests.
     * Not to be used in production code
     */
    @Deprecated
    Stork() {
        Map<String, LoadBalancerProvider> loadBalancerProviders = getAll(LoadBalancerProvider.class);
        Map<String, ServiceDiscoveryProvider> serviceDiscoveryProviders = getAll(ServiceDiscoveryProvider.class);

        ServiceLoader<ConfigProvider> configs = ServiceLoader.load(ConfigProvider.class);
        // TODO test for multiple config providers!!
        Optional<ConfigProvider> highestPrioConfigProvider = configs.stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(ConfigProvider::priority));

        ConfigProvider configProvider = highestPrioConfigProvider.orElseThrow(
                () -> new IllegalStateException("No SmallRye Stork ConfigProvider found"));

        for (ServiceConfig serviceConfig : configProvider.getConfigs()) {
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

            final var serviceDiscovery = serviceDiscoveryProvider.createServiceDiscovery(serviceDiscoveryConfig,
                    serviceConfig.serviceName());

            final var loadBalancerConfig = serviceConfig.loadBalancer();
            final LoadBalancer loadBalancer;
            if (loadBalancerConfig == null) {
                // no load balancer, maybe someone intends to use service discovery only, ignoring
                LOGGER.info("No load balancer configured for type " + serviceDiscoveryType);
                loadBalancer = null;
            } else {
                String loadBalancerType = loadBalancerConfig.type();
                final var loadBalancerProvider = loadBalancerProviders.get(loadBalancerType);
                if (loadBalancerProvider == null) {
                    throw new IllegalArgumentException("No LoadBalancerProvider for type " + loadBalancerType);
                }

                loadBalancer = loadBalancerProvider.createLoadBalancer(loadBalancerConfig, serviceDiscovery);
            }

            services.put(serviceConfig.serviceName(), new Service(serviceConfig.serviceName(), loadBalancer, serviceDiscovery));
        }
    }

    private <T extends ElementWithType> Map<String, T> getAll(Class<T> providerClass) {
        ServiceLoader<T> providers = ServiceLoader.load(providerClass);
        return providers.stream().map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(ElementWithType::type, Function.identity()));
    }

    private static final Stork stork = new Stork();

    public static Stork getInstance() {
        return stork;
    }
}
