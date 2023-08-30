package io.smallrye.stork.api.observability;

import java.time.Duration;
import java.util.List;

import io.smallrye.stork.api.ServiceInstance;

public interface ObservationPoints {

    class StorkResolutionEvent {
        // Handler / Reporter
        private final ObservationCollector.EventCompletionHandler handler;

        // Metadata
        private final String serviceName;
        private final String serviceDiscoveryType;
        private final String serviceSelectionType;

        // Time
        private final long begin;
        private volatile long endOfServiceDiscovery;
        private volatile long endOfServiceSelection;

        // Service discovery data
        volatile int instancesCount = -1;

        // Service selection data
        volatile long selectedInstance = -1L;

        // Overall status
        volatile boolean succeeded;
        volatile Throwable failure;

        public StorkResolutionEvent(String serviceName, String serviceDiscoveryType, String serviceSelectionType,
                ObservationCollector.EventCompletionHandler handler) {
            this.handler = handler;
            this.serviceName = serviceName;
            this.serviceDiscoveryType = serviceDiscoveryType;
            this.serviceSelectionType = serviceSelectionType;
            this.begin = System.nanoTime();
        }

        public void onServiceDiscoverySuccess(List<ServiceInstance> instances) {
            this.endOfServiceDiscovery = System.nanoTime();
            this.serviceDiscoveryDone = true;
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
            this.selectedInstance = id;
            this.succeeded = true;
            this.handler.complete(this);
        }

        public void onServiceSelectionFailure(Throwable throwable) {
            this.endOfServiceSelection = System.nanoTime();
            this.failure = throwable;
            this.handler.complete(this);
        }

        public boolean isDone() {
            return succeeded || failure != null;
        }

        public Duration getOverallDuration() {
            if (!isDone()) {
                return null;
            }
            return Duration.ofNanos(endOfServiceSelection - begin);
        }

        public Duration getServiceDiscoveryDuration() {
            if (!isDone()) {
                return null;
            }
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
    }

}
