package io.smallrye.stork;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.MetadataKey;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.NoSuchServiceRegistrarException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.StorkServiceRegistry;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.impl.RoundRobinLoadBalancer;
import io.smallrye.stork.impl.RoundRobinLoadBalancerProvider;
import io.smallrye.stork.integration.DefaultStorkInfrastructure;
import io.smallrye.stork.spi.ElementWithType;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.config.ConfigProvider;
import io.smallrye.stork.spi.config.SimpleServiceConfig;
import io.smallrye.stork.spi.internal.LoadBalancerLoader;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;
import io.smallrye.stork.spi.internal.ServiceRegistrarLoader;

/**
 * The entrypoint for SmallRye Stork.
 * <p>
 * Use `Stork.getInstance()` to get a hold of a configured instance, and retrieve `ServiceDiscovery` and/or `LoadBalancer`
 * from it.
 */
public final class Stork implements StorkServiceRegistry {
    // TODO replace all the exceptions here with dedicated ones?

    /**
     * The stork name.
     */
    public static final String STORK = "stork";

    /**
     * configuration prefix for stork service registrars
     */
    public static final String STORK_REGISTRAR = "stork-registrar";

    private static final Logger LOGGER = LoggerFactory.getLogger(Stork.class);

    private final Map<String, Service> services = new ConcurrentHashMap<>();
    private final Map<String, ServiceRegistrar> serviceRegistrars = new ConcurrentHashMap<>();
    private final StorkInfrastructure infrastructure;

    @Override
    public Service getService(String serviceName) {
        Service service = services.get(serviceName);
        if (service == null) {
            throw new NoSuchServiceDefinitionException(serviceName);
        }
        return service;
    }

    @Override
    public Optional<Service> getServiceOptional(String serviceName) {
        Service service = services.get(serviceName);
        return Optional.ofNullable(service);
    }

    @Override
    public Map<String, Service> getServices() {
        return Collections.unmodifiableMap(services);
    }

    @Override
    public Stork defineIfAbsent(String name, ServiceDefinition definition) {
        ParameterValidation.nonNull(name, "name");
        ParameterValidation.nonNull(definition, "definition");
        // The service definition has already been validated during its construction
        // (so the discovery is not null)

        ServiceConfig config = toServiceConfig(name, definition);
        Service service = createService(config);
        services.putIfAbsent(name, service);
        service.getServiceDiscovery().initialize(this);
        return this;
    }

    private ServiceConfig toServiceConfig(String name, ServiceDefinition definition) {
        return new SimpleServiceConfig.Builder().setServiceName(name)
                .setLoadBalancer(definition.getLoadBalancer())
                .setServiceDiscovery(definition.getServiceDiscovery())
                .build();
    }

    /**
     * Exposed for tests.
     * Not to be used in production code
     *
     * @param storkInfrastructure the infrastructure, must not be {@code null}
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public Stork(StorkInfrastructure storkInfrastructure) {
        this.infrastructure = storkInfrastructure;
        Map<String, LoadBalancerLoader> loadBalancerLoaders = loadFromServiceLoader(LoadBalancerLoader.class);
        Map<String, ServiceDiscoveryLoader> serviceDiscoveryLoaders = loadFromServiceLoader(ServiceDiscoveryLoader.class);
        Map<String, ServiceRegistrarLoader> registrarLoaders = loadFromServiceLoader(ServiceRegistrarLoader.class);

        extendWithCdiLoaders(serviceDiscoveryLoaders, loadBalancerLoaders, registrarLoaders);

        ConfigProvider configProvider = lookForConfigProvider();
        if (configProvider != null) {
            for (ServiceConfig serviceConfig : configProvider.getConfigs()) {
                Service service = createService(loadBalancerLoaders, serviceDiscoveryLoaders, registrarLoaders, serviceConfig);

                services.put(serviceConfig.serviceName(), service);
            }
            for (Service service : services.values()) {
                service.getServiceDiscovery().initialize(this);
            }
        }

    }

    private void extendWithCdiLoaders(Map<String, ServiceDiscoveryLoader> serviceDiscoveryLoaders,
            Map<String, LoadBalancerLoader> loadBalancerLoaders, Map<String, ServiceRegistrarLoader> registrarLoaders) {
        CDI cdi;
        try {
            cdi = CDI.current();
        } catch (IllegalStateException e) {
            // Not a CDI environment
            return;
        }

        Instance<ServiceDiscoveryLoader> sdl = cdi.select(ServiceDiscoveryLoader.class);
        Instance<LoadBalancerLoader> lbl = cdi.select(LoadBalancerLoader.class);
        Instance<ServiceRegistrarLoader> srl = cdi.select(ServiceRegistrarLoader.class);
        sdl.forEach(l -> serviceDiscoveryLoaders.put(l.type(), l));
        lbl.forEach(l -> loadBalancerLoaders.put(l.type(), l));
        srl.forEach(l -> registrarLoaders.put(l.type(), l));
    }

    private static ConfigProvider lookForConfigProvider() {
        List<ConfigProvider> providers = ServiceLoader.load(ConfigProvider.class)
                .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());

        try {
            var cdi = CDI.current();
            providers.addAll(cdi.select(ConfigProvider.class).stream().collect(Collectors.toList()));
        } catch (IllegalStateException e) {
            // Ignored - no cdi.
        }
        Optional<ConfigProvider> highestPrioConfigProvider = providers.stream()
                .max(Comparator.comparingInt(ConfigProvider::priority));
        return highestPrioConfigProvider.orElse(null);
    }

    private Service createService(ServiceConfig serviceConfig) {
        return createService(loadFromServiceLoader(LoadBalancerLoader.class),
                loadFromServiceLoader(ServiceDiscoveryLoader.class),
                loadFromServiceLoader(ServiceRegistrarLoader.class), serviceConfig);
    }

    private Service createService(Map<String, LoadBalancerLoader> loadBalancerLoaders,
            Map<String, ServiceDiscoveryLoader> serviceDiscoveryProviders,
            Map<String, ServiceRegistrarLoader> serviceRegistrarLoaders, ServiceConfig serviceConfig) {
        var serviceDiscoveryConfig = serviceConfig.serviceDiscovery();
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

        if (serviceConfig.secure()) {
            // Backward compatibility
            LOGGER.warn("The 'secure' attribute is deprecated, use the 'secure' service discovery attribute instead");
            // We do not know if we can add to the parameters, such create a new SimpleConfigWithType
            Map<String, String> newConfig = new HashMap<>(serviceDiscoveryConfig.parameters());
            newConfig.put("secure", "true");
            serviceDiscoveryConfig = new SimpleServiceConfig.SimpleServiceDiscoveryConfig(serviceDiscoveryType, newConfig);
        }

        final var serviceDiscovery = serviceDiscoveryProvider.createServiceDiscovery(serviceDiscoveryConfig,
                serviceConfig.serviceName(), serviceConfig, infrastructure);

        final var loadBalancerConfig = serviceConfig.loadBalancer();
        final LoadBalancer loadBalancer;
        String loadBalancerType;
        if (loadBalancerConfig == null) {
            // no load balancer, use round-robin
            LOGGER.debug("No load balancer configured for type {}, using {}", serviceDiscoveryType,
                    RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE);
            loadBalancerType = RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE;
            loadBalancer = new RoundRobinLoadBalancer();
        } else {
            loadBalancerType = loadBalancerConfig.type();
            final var loadBalancerProvider = loadBalancerLoaders.get(loadBalancerType);
            if (loadBalancerProvider == null) {
                throw new IllegalArgumentException("No LoadBalancerProvider for type " + loadBalancerType);
            }

            loadBalancer = loadBalancerProvider.createLoadBalancer(loadBalancerConfig, serviceDiscovery);
        }

        final var serviceRegistrarConfig = serviceConfig.serviceRegistrar();
        ServiceRegistrar<?> serviceRegistrar = null;
        if (serviceRegistrarConfig == null) {
            LOGGER.debug("No service registrar configured for service {}", serviceConfig.serviceName());
        } else {
            String serviceRegistrarType = serviceRegistrarConfig.type();
            final var serviceRegistrarLoader = serviceRegistrarLoaders.get(serviceRegistrarType);
            if (serviceRegistrarLoader == null) {
                throw new IllegalArgumentException("No ServiceRegistrarLoader for type " + serviceRegistrarType);
            }

            serviceRegistrar = serviceRegistrarLoader.createServiceRegistrar(serviceRegistrarConfig,
                    serviceConfig.serviceName(), infrastructure);
        }

        return new Service(serviceConfig.serviceName(),
                loadBalancerType, serviceDiscoveryType, infrastructure.getObservationCollector(),
                loadBalancer, serviceDiscovery, serviceRegistrar,
                loadBalancer.requiresStrictRecording());
    }

    private <T extends ElementWithType> Map<String, T> loadFromServiceLoader(Class<T> loaderClass) {
        ServiceLoader<T> loader = ServiceLoader.load(loaderClass);
        return loader.stream().map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(ElementWithType::type, Function.identity()));
    }

    private static final AtomicReference<Stork> REFERENCE = new AtomicReference<>();

    /**
     * @return the stork instance.
     */
    public static Stork getInstance() {
        return REFERENCE.get();
    }

    /**
     * Closes the stork instance.
     */
    public static void shutdown() {
        var previous = REFERENCE.getAndSet(null);
        if (previous != null) {
            previous.clear();
        }
    }

    private void clear() {
        services.clear();
        serviceRegistrars.clear();
    }

    /**
     * Initialize the stork instance using the given infrastructure.
     *
     * @param infrastructure the infrastructure, must not be {@code null}
     */
    public static void initialize(StorkInfrastructure infrastructure) {
        REFERENCE.compareAndSet(null, new Stork(infrastructure));
    }

    /**
     * Initialize the stork instance using the default infrastructure.
     */
    public static void initialize() {
        initialize(new DefaultStorkInfrastructure());
    }

    @SuppressWarnings("unchecked")
    /**
     * Whether the communication should use a secure connection (e.g. HTTPS)
     *
     * @return true if SSL, TLS, etc. should be used for the communication
     * @deprecated Use stork.getService("my-service").getServiceRegistrar() method instead
     */
    @Deprecated
    public <MetadataKeyType extends Enum<MetadataKeyType> & MetadataKey> ServiceRegistrar<MetadataKeyType> getServiceRegistrar(
            String registrarName) {
        ServiceRegistrar registrar = serviceRegistrars.get(registrarName);
        if (registrar == null) {
            throw new NoSuchServiceRegistrarException(registrarName);
        }
        return registrar;
    }
}
