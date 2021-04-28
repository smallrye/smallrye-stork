package io.smallrye.discovery;

import java.util.Collections;

public class StaticListServiceDiscoveryProducer implements ServiceDiscoveryProducer {

    public StaticListServiceDiscovery getServiceDiscovery(String serviceName) {
        // TODO load the addresses from the configuration
        return new StaticListServiceDiscovery(Collections.emptyList());
    }
}
