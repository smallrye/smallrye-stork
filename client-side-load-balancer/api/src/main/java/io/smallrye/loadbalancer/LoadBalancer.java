package io.smallrye.loadbalancer;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.smallrye.discovery.ServiceInstance;

public interface LoadBalancer {
    CompletionStage<ServiceInstance> getServiceInstance();

    Optional<ServiceInstance> getServiceInstanceBlocking();

    void registerExecution(ServiceInstance instance, long executionTimeMillis, Throwable error);
}
