package io.smallrye.loadbalancer;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.Config;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final TargetAddressProvider addressListProvider;
    private final AtomicLong addressIndex = new AtomicLong(0);

    RoundRobinLoadBalancer(Config config, TargetAddressProvider addressProvider) {
        this.addressListProvider = addressProvider;
    }

    @Override
    public void registerExecution(TargetAddress address, long executionTimeMillis, Throwable error) {
        // this is a simple load balancer that doesn't care about failures, etc
    }

    @Override
    public CompletionStage<TargetAddress> getNextAddress() {
        return addressListProvider.getAddressList().thenApply(
                list -> list.get((int) (addressIndex.getAndIncrement() % list.size()))
        );
    }
}
