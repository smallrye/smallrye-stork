package io.smallrye.stork.api.observability;

import io.smallrye.stork.api.ServiceInstance;

import java.time.Duration;
import java.util.List;

public class StorkResolutionEvent {
    // Handler / Reporter
    protected final EventCompletionHandler handler;

    // Metadata
    protected final String serviceName;
    protected final String serviceDiscoveryType;
    protected final String serviceSelectionType;

    // Time
    protected final long begin;
    protected volatile long endOfServiceDiscovery;
    protected volatile long endOfServiceSelection;

    // Service discovery data
    protected volatile int instancesCount = -1;

    // Service selection data
    protected volatile long selectedInstanceId = -1L;

    // Overall status
    protected volatile boolean done;
    protected volatile boolean serviceDiscoverySuccessful = false;
    protected volatile Throwable failure;

    public StorkResolutionEvent(String serviceName, String serviceDiscoveryType, String serviceSelectionType,
                                EventCompletionHandler handler) {
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
        this.handler.complete(this);
    }

    public void onServiceSelectionSuccess(long id) {
        this.endOfServiceSelection = System.nanoTime();
        this.selectedInstanceId = id;
        this.done = true;
        this.handler.complete(this);
    }

    public void onServiceSelectionFailure(Throwable throwable) {
        this.endOfServiceSelection = System.nanoTime();
        this.failure = throwable;
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

    public long getSelectedInstanceId() {
        return selectedInstanceId;
    }

    public boolean isServiceDiscoverySuccessful() {
        return serviceDiscoverySuccessful;
    }
}
