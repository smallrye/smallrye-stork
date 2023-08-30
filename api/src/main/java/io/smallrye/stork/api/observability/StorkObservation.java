package io.smallrye.stork.api.observability;

import java.time.Duration;
import java.util.List;

import io.smallrye.stork.api.ServiceInstance;

public class StorkObservation {
    // Handler / Reporter
    private final StorkEventHandler handler;

    // Metadata
    private final String serviceName;
    private final String serviceDiscoveryType;
    private final String serviceSelectionType;

    // Time
    private final long begin;
    private volatile long endOfServiceDiscovery;
    private volatile long endOfServiceSelection;

    // Service discovery data
    private volatile int instancesCount = -1;

    // Service selection data
    private volatile long selectedInstanceId = -1L;

    // Overall status
    private volatile boolean done;
    private volatile boolean serviceDiscoverySuccessful = false;
    private volatile Throwable failure;

    public StorkObservation(String serviceName, String serviceDiscoveryType, String serviceSelectionType,
            StorkEventHandler handler) {
        this.handler = handler;
        this.serviceName = serviceName;
        this.serviceDiscoveryType = serviceDiscoveryType;
        this.serviceSelectionType = serviceSelectionType;
        this.begin = System.nanoTime();
    }

    public void onServiceDiscoverySuccess(List<ServiceInstance> instances) {
        this.endOfServiceDiscovery = System.nanoTime();
        this.serviceDiscoverySuccessful = true;
        if (instances != null) {
            this.instancesCount = instances.size();
        } else {
            this.instancesCount = 0;
        }
    }

    public void onServiceDiscoveryFailure(Throwable throwable) {
        this.endOfServiceDiscovery = System.nanoTime();
        this.failure = throwable;
    }

    public void onServiceSelectionSuccess(long id) {
        this.endOfServiceSelection = System.nanoTime();
        this.selectedInstanceId = id;
        this.done = true;
        this.handler.complete(this);
    }

    public void onServiceSelectionFailure(Throwable throwable) {
        this.endOfServiceSelection = System.nanoTime();
        if (failure != throwable) {
            this.failure = throwable;
        }
        this.handler.complete(this);
    }

    public boolean isDone() {
        return done || failure != null;
    }

    public Duration getOverallDuration() {
        if (!isDone()) {
            return null;
        }
        return Duration.ofNanos(endOfServiceSelection - begin);
    }

    public Duration getServiceDiscoveryDuration() {
        return Duration.ofNanos(endOfServiceDiscovery - begin);
    }

    public Duration getServiceSelectionDuration() {
        if (!isDone()) {
            return null;
        }
        return Duration.ofNanos(endOfServiceSelection - endOfServiceDiscovery);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceDiscoveryType() {
        return serviceDiscoveryType;
    }

    public String getServiceSelectionType() {
        return serviceSelectionType;
    }

    public int getDiscoveredInstancesCount() {
        return instancesCount;
    }

    public Throwable failure() {
        return failure;
    }

    public boolean isServiceDiscoverySuccessful() {
        return serviceDiscoverySuccessful;
    }
}
