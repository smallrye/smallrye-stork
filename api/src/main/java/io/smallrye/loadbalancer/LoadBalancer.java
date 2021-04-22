package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface LoadBalancer {

    void registerExecution(TargetAddress address, long executionTimeMillis,
                           Throwable error);

    default TargetAddress getNextAddressBlocking() throws ExecutionException, InterruptedException, TimeoutException {
        return getNextAddress().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }

    CompletionStage<TargetAddress> getNextAddress();
}
