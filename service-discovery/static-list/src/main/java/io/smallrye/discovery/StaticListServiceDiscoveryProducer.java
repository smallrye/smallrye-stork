package io.smallrye.discovery;

import java.util.Collections;

import javax.enterprise.inject.Produces;

public class StaticListServiceDiscoveryProducer {

    @Produces
    public StaticListServiceDiscovery staticListServiceDiscovery() {
        // TODO load the addresses from the configuration
        return new StaticListServiceDiscovery(Collections.emptyList());
    }
}
