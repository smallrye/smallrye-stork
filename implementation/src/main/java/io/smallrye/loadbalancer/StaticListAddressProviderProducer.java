package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StaticListAddressProviderProducer implements TargetAddressProviderProducer {

    private final List<TargetAddress> addresses = new ArrayList<>();

    private final Map<String, TargetAddressProvider> providerCache = new ConcurrentHashMap<>();

    @Override
    public TargetAddressProvider getTargetAddressProvider(Config config, String name) {
        return providerCache.computeIfAbsent(name, ignored -> createProvider(config));
    }

    private TargetAddressProvider createProvider(Config config) {
        String addressesAsString = config.getValue("addresses", String.class);
        String[] addressArray = addressesAsString.split(",");

        long id = 0;
        for (String address : addressArray) {
            addresses.add(new TargetAddress(URI.create(address), id++));
        }

        final CompletableFuture<List<TargetAddress>> result = CompletableFuture.completedFuture(addresses);

        return () -> result;
    }
}
