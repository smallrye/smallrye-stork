package io.smallrye.dux.spi;

import java.util.Collections;
import java.util.Map;

import io.smallrye.dux.config.LoadBalancerConfig;
import io.smallrye.dux.config.ServiceConfig;
import io.smallrye.dux.config.ServiceDiscoveryConfig;

public class SimpleServiceConfig implements ServiceConfig {

    private final String serviceName;

    private final LoadBalancerConfig loadBalancerConfig;
    private final ServiceDiscoveryConfig serviceDiscoveryConfig;

    private SimpleServiceConfig(String serviceName, LoadBalancerConfig loadBalancerConfig,
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

    public static class Builder {
        String serviceName;
        LoadBalancerConfig loadBalancerConfig;
        ServiceDiscoveryConfig serviceDiscoveryConfig;

        public Builder setLoadBalancer(LoadBalancerConfig loadBalancer) {
            loadBalancerConfig = loadBalancer;
            return this;
        }

        public Builder setServiceDiscovery(ServiceDiscoveryConfig serviceDiscovery) {
            serviceDiscoveryConfig = serviceDiscovery;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public SimpleServiceConfig build() {
            return new SimpleServiceConfig(serviceName, loadBalancerConfig, serviceDiscoveryConfig);
        }
    }

    public static class SimpleLoadBalancerConfig implements LoadBalancerConfig {

        private final String type;
        private final Map<String, String> parameters;

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

    public static class SimpleServiceDiscoveryConfig implements ServiceDiscoveryConfig {
        private final String type;
        private final Map<String, String> parameters;

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
