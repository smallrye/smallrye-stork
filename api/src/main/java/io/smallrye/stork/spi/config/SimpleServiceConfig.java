package io.smallrye.stork.spi.config;

import java.util.Collections;
import java.util.Map;

import io.smallrye.stork.api.config.LoadBalancerConfig;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.api.config.ServiceDiscoveryConfig;

/**
 * Implementation of {@link ServiceConfig} storing the service name, service discovery config and load balancer config.
 * Instances should be created using {@link Builder}.
 */
public class SimpleServiceConfig implements ServiceConfig {

    private final String serviceName;

    private final LoadBalancerConfig loadBalancerConfig;
    private final ServiceDiscoveryConfig serviceDiscoveryConfig;

    private SimpleServiceConfig(String serviceName,
            LoadBalancerConfig loadBalancerConfig,
            ServiceDiscoveryConfig serviceDiscoveryConfig) {
        this.serviceName = serviceName;
        this.loadBalancerConfig = loadBalancerConfig;
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public LoadBalancerConfig loadBalancer() {
        return loadBalancerConfig;
    }

    @Override
    public ServiceDiscoveryConfig serviceDiscovery() {
        return serviceDiscoveryConfig;
    }

    /**
     * A {@link SimpleServiceConfig} builder.
     */
    public static class Builder {
        String serviceName;
        LoadBalancerConfig loadBalancerConfig;
        ServiceDiscoveryConfig serviceDiscoveryConfig;
        boolean secure;

        /**
         * Sets the load balancer config.
         *
         * @param loadBalancer the load balancer config
         * @return the current builder
         */
        public Builder setLoadBalancer(LoadBalancerConfig loadBalancer) {
            loadBalancerConfig = loadBalancer;
            return this;
        }

        /**
         * Sets the service discovery config.
         *
         * @param serviceDiscovery the service discovery config
         * @return the current builder
         */
        public Builder setServiceDiscovery(ServiceDiscoveryConfig serviceDiscovery) {
            serviceDiscoveryConfig = serviceDiscovery;
            return this;
        }

        /**
         * Sets the service name.
         *
         * @param serviceName the service name, must not be {@code null} or blank.
         * @return the current builder
         */
        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * Sets to {@code true} to indicate that the service uses a <em>secure transport</em> (TLS).
         *
         * @param secure {@code true} to indicate that the service uses TLS.
         * @return the current builder
         */
        public Builder setSecure(boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * Creates the {@link SimpleServiceConfig}
         *
         * @return the built config
         */
        public SimpleServiceConfig build() {
            return new SimpleServiceConfig(serviceName, loadBalancerConfig, serviceDiscoveryConfig);
        }
    }

    /**
     * An implementation of {@link LoadBalancerConfig} using an unmodifiable {@link Map} as backend to store
     * the configuration.
     */
    public static class SimpleLoadBalancerConfig implements LoadBalancerConfig {

        private final String type;
        private final Map<String, String> parameters;

        /**
         * Creates a new SimpleLoadBalancerConfig.
         *
         * @param type the type
         * @param parameters the configuration map
         */
        public SimpleLoadBalancerConfig(String type, Map<String, String> parameters) {
            this.type = type;
            this.parameters = Collections.unmodifiableMap(parameters);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public Map<String, String> parameters() {
            return parameters;
        }
    }

    /**
     * An implementation of {@link ServiceDiscoveryConfig} using an unmodifiable {@link Map} as backend to store
     * the configuration.
     */
    public static class SimpleServiceDiscoveryConfig implements ServiceDiscoveryConfig {
        private final String type;
        private final Map<String, String> parameters;

        /**
         * Creates a new SimpleServiceDiscoveryConfig.
         *
         * @param type the type
         * @param parameters the configuration map
         */
        public SimpleServiceDiscoveryConfig(String type, Map<String, String> parameters) {
            this.type = type;
            this.parameters = Collections.unmodifiableMap(parameters);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public Map<String, String> parameters() {
            return parameters;
        }
    }

}
