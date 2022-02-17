package io.smallrye.stork;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.impl.RoundRobinLoadBalancer;
import io.smallrye.stork.impl.RoundRobinLoadBalancerProvider;
import io.smallrye.stork.integration.DefaultStorkInfrastructure;
import io.smallrye.stork.spi.ElementWithType;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.config.ConfigProvider;
import io.smallrye.stork.spi.internal.LoadBalancerLoader;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;

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
    public Stork(StorkInfrastructure storkInfrastructure) {
        Map<String, LoadBalancerLoader> loadBalancerProviders = getAll(LoadBalancerLoader.class);
        Map<String, ServiceDiscoveryLoader> serviceDiscoveryProviders = getAll(ServiceDiscoveryLoader.class);

        ServiceLoader<ConfigProvider> configs = ServiceLoader.load(ConfigProvider.class);
        Optional<ConfigProvider> highestPrioConfigProvider = configs.stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(ConfigProvider::priority));

        ConfigProvider configProvider = highestPrioConfigProvider.orElseThrow(
                () -> new IllegalStateException("No SmallRye Stork ConfigProvider found"));

        for (ServiceConfig serviceConfig : configProvider.getConfigs()) {
            final var serviceDiscoveryConfig = serviceConfig.serviceDiscovery();
            if (serviceDiscoveryConfig == null) {
                throw new IllegalArgumentException(
                        "No service discovery defined for service " + serviceConfig.serviceName());
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
                    serviceConfig.serviceName(), serviceConfig, storkInfrastructure);

            final var loadBalancerConfig = serviceConfig.loadBalancer();
            final LoadBalancer loadBalancer;
            if (loadBalancerConfig == null) {
                // no load balancer, use round-robin
                LOGGER.info("No load balancer configured for type {}, using {}", serviceDiscoveryType,
                        RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE);
                loadBalancer = new RoundRobinLoadBalancer();
            } else {
                String loadBalancerType = loadBalancerConfig.type();
                final var loadBalancerProvider = loadBalancerProviders.get(loadBalancerType);
                if (loadBalancerProvider == null) {
                    throw new IllegalArgumentException("No LoadBalancerProvider for type " + loadBalancerType);
                }

                loadBalancer = loadBalancerProvider.createLoadBalancer(loadBalancerConfig, serviceDiscovery);
            }

            services.put(serviceConfig.serviceName(),
                    new Service(serviceConfig.serviceName(), loadBalancer, serviceDiscovery,
                            serviceConfig.secure(),
                            loadBalancer.requiresStrictRecording()));
        }
        for (Service service : services.values()) {
            service.getServiceDiscovery().initialize(this.services);
        }

    }

    private <T extends ElementWithType> Map<String, T> getAll(Class<T> providerClass) {
        ServiceLoader<T> providers = ServiceLoader.load(providerClass);
        return providers.stream().map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(ElementWithType::type, Function.identity()));
    }

    private static final AtomicReference<Stork> REFERENCE = new AtomicReference<>();

    public static Stork getInstance() {
        return REFERENCE.get();
    }

    public static void shutdown() {
        REFERENCE.set(null);
    }

    public static void initialize(StorkInfrastructure infrastructure) {
        REFERENCE.compareAndSet(null, new Stork(infrastructure));
    }

    public static void initialize() {
        initialize(new DefaultStorkInfrastructure());
    }
}
