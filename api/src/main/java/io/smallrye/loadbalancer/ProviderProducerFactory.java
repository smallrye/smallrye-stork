package io.smallrye.loadbalancer;

public interface ProviderProducerFactory {
    TargetAddressProvider get(String name);
}
