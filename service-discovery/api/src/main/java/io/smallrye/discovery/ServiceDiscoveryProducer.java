package io.smallrye.discovery;

public interface ServiceDiscoveryProducer {
    ServiceDiscovery getServiceDiscovery(String serviceName);
}
