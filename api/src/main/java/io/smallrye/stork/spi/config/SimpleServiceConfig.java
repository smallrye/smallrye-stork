package io.smallrye.stork.spi.config;

import java.util.Collections;
import java.util.Map;

import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;

/**
 * Implementation of {@link ServiceConfig} storing the service name, service discovery config and load balancer config.
 * Instances should be created using {@link Builder}.
 */
public class SimpleServiceConfig implements ServiceConfig {

    private final String serviceName;

    private final ConfigWithType loadBalancerConfig;
    private final ConfigWithType serviceDiscoveryConfig;

    private final ConfigWithType serviceRegistrarConfig;

    private SimpleServiceConfig(String serviceName,
            ConfigWithType loadBalancerConfig,
            ConfigWithType serviceDiscoveryConfig, ConfigWithType serviceRegistrarConfig) {
        this.serviceName = serviceName;
        this.loadBalancerConfig = loadBalancerConfig;
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
        this.serviceRegistrarConfig = serviceRegistrarConfig;
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public ConfigWithType loadBalancer() {
        return loadBalancerConfig;
    }

    @Override
    public ConfigWithType serviceDiscovery() {
        return serviceDiscoveryConfig;
    }

    @Override
    public ConfigWithType serviceRegistrar() {
        return serviceRegistrarConfig;
    }

    /**
     * A {@link SimpleServiceConfig} builder.
     */
    public static class Builder {
        String serviceName;
        ConfigWithType loadBalancerConfig;
        ConfigWithType serviceDiscoveryConfig;

        ConfigWithType serviceRegistrarConfig;
        boolean secure;

        /**
         * Sets the load balancer config.
         *
         * @param loadBalancer the load balancer config
         * @return the current builder
         */
        public Builder setLoadBalancer(ConfigWithType loadBalancer) {
            loadBalancerConfig = loadBalancer;
            return this;
        }

        /**
         * Sets the service discovery config.
         *
         * @param serviceDiscoveryConfig the service discovery config
         * @return the current builder
         */
        public Builder setServiceDiscovery(ConfigWithType serviceDiscoveryConfig) {
            this.serviceDiscoveryConfig = serviceDiscoveryConfig;
            return this;
        }

        /**
         * Sets the service registrar config.
         *
         * @param serviceRegistrarConfig the service registrar config
         * @return the current builder
         */
        public Builder setServiceRegistrar(ConfigWithType serviceRegistrarConfig) {
            this.serviceRegistrarConfig = serviceRegistrarConfig;
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
            return new SimpleServiceConfig(serviceName, loadBalancerConfig, serviceDiscoveryConfig, serviceRegistrarConfig);
        }
    }

    /**
     * An implementation of {@link ConfigWithType} using an unmodifiable {@link Map} as backend to store
     * the configuration.
     */
    public static class SimpleLoadBalancerConfig implements ConfigWithType {

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
     * An implementation of {@link ConfigWithType} using an unmodifiable {@link Map} as backend to store
     * the configuration.
     */
    public static class SimpleServiceDiscoveryConfig implements ConfigWithType {
        private final String type;
        private final Map<String, String> parameters;

        /**
         * Creates a new SimpleConfigWithType.
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

    /**
     * An implementation of {@link ConfigWithType} using an unmodifiable {@link Map} as backend to store
     * the configuration.
     */
    public static class SimpleServiceRegistrarConfig implements ConfigWithType {
        private final String type;
        private final Map<String, String> parameters;

        /**
         * Creates a new SimpleServiceRegistrarConfig.
         *
         * @param type the type
         * @param parameters the configuration map
         */
        public SimpleServiceRegistrarConfig(String type, Map<String, String> parameters) {
            this.type = type;
            this.parameters = parameters;
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
