package io.smallrye.stork;

import java.time.LocalDateTime;
import java.util.List;

public class ServiceInstancesCache {

    private List<ServiceInstance> serviceInstances;
    private LocalDateTime lastFetchDateTime;

    public ServiceInstancesCache(List<ServiceInstance> serviceInstances, LocalDateTime lastFetchDateTime) {
        this.serviceInstances = serviceInstances;
        this.lastFetchDateTime = lastFetchDateTime;
    }

    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }

    public void setServiceInstances(List<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    public LocalDateTime getLastFetchDateTime() {
        return lastFetchDateTime;
    }

    public void setLastFetchDateTime(LocalDateTime lastFetchDateTime) {
        this.lastFetchDateTime = lastFetchDateTime;
    }
}
